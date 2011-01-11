package net.rcode.assetserver.ejs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses an Ejs source document into a sequence of events representing
 * its structure.
 * 
 * <h2>Structure</h2>
 * The EJS grammar consist of the following elements:
 * <ul>
 * <li>command: turns ejs grammar on and off.  If off, everything is taken
 * 		as a literal
 * <li>literal: runs of literal text
 * <li>directive: single line (or multi-adjacent lines) of javascript
 * <li>block: delimitted block of JavaScript
 * <li>interpolation: inline JavaScript expression whose result is output
 * </ul>
 * 
 * <h3>Commands</h3>
 * There are two commands:
 * <pre>
 *  ##EJSDISABLE
 * 	##EJSON
 *  ##EJSOFF
 * </pre>
 * Each command must exist on its own line with nothing but whitespace
 * separating it from adjacent lines.  The stream starts in "OFF" mode,
 * whereby everything encountered is considered a literal.  The presense
 * of an ##EJSON command marks the following region (or remainder of the stream)
 * as subject to EJS parsing.
 * <p>
 * Using an EJSDISABLE command will disable all processing (including the scanning
 * for commands) for the remainder of the input.
 * 
 * <h3>Literal</h3>
 * Anything not recognized as another structured element is assumed to be a literal.
 * 
 * <h3>Directive</h3>
 * Single line directives can be introduced with a line starting with "##" just like
 * commands, but where the text following the "##" is not a command token.  Multiple
 * adjacent lines of directives are concatenated together to form a compilation unit
 * that is evaluated at the given point of processing the stream.  Note that unlike other
 * template languages that encourage spreading language structures across code areas,
 * this is not permitted in EJS.  Each run of single line directives is compiled into
 * its own unit, which restricts the ability to interleave program structure with literal
 * text.
 * 
 * <h3>Block</h3>
 * A multi-line compilation unit of script can be delimitted with matcing pairs of
 * "##=" starting a line containing nothing but the delimitter.  This is exactly an
 * identical effect as multiple adjacent lines of single-line directives.
 * 
 * <h3>Interpolation</h3>
 * Anywhere within a literal, a pattern of #{...} can be placed.  The contents within the
 * braces are evaluated and the result (converted to a String) is output in place of the 
 * interpolation sequence.  Note that the parser does support scanning for a matching
 * end brace, skipping embedded string literals and balanced internal braces.
 * 
 * <h3>Escaping</h3>
 * All introductory sequences are introduced with a hash sign (#).  In order to output
 * a literal introductory sequence, double the hash sign.  The extra hash sign will be
 * stripped in the output.  Outputting the following sequences can be performed in this
 * manner:
 * <ul>
 * <li>Double hash at start of line: "###"
 * <li>Interpolation start (#{): "##{"
 * </ul>
 * No other syntax collisions exist.  The above patterns do not exist in most code, so
 * conflict should be rare.  But if including large quantities of opaque literal text,
 * surrounding the region with "##EJSOFF" and "##EJSON" is safest.
 * <p>
 * Note that with respect to interpolations, only the literal pattern "#{" is recognized.
 * Single hash signs not followed by a brace are unambiguous and cannot be escaped.
 * 
 * @author stella
 *
 */
public class EjsParser {

	public static class LocationInfo {
		private CharSequence source;
		private int lineStart;
		private int sourceStart;
		private int sourceEnd;
		
		public LocationInfo() {
		}
		
		public CharSequence getSource() {
			return source;
		}
		
		public int getLineStart() {
			return lineStart;
		}
		
		public int getSourceStart() {
			return sourceStart;
		}
		public int getSourceEnd() {
			return sourceEnd;
		}
	}
	
	/**
	 * Events to be output
	 * @author stella
	 */
	public static interface Events {
		/**
		 * A literal was encountered
		 * @param text
		 * @param location
		 */
		public void handleLiteral(CharSequence text, LocationInfo location);
		
		/**
		 * A block of code (either multi-line or run of single line) was
		 * encountered.
		 * @param script
		 * @param location
		 */
		public void handleBlock(CharSequence script, LocationInfo location);
		
		/**
		 * An interpolation was encountered
		 * @param script
		 * @param location
		 */
		public void handleInterpolation(CharSequence script, LocationInfo location);
	}
	
	private Events events;
	private LocationInfo location;
	
	private CharSequence source;
	private Matcher matcher;
	private int position;
	private int lineNumber;
	private StringBuilder buffer;
	
	/**
	 * Matches a "top-level" command (EJSON or EJSDISABLE) that can be
	 * encountered when the parser is in a disabled state.
	 * <h3>Groups</h3>
	 * <ul>
	 * <li>Group 1: If non-empty, then this is an extra escape pound character,
	 *     meaning that even though the sequence matched, it should be considered
	 *     escaped.  Strip one pound out of the entire match and output as a literal
	 * <li>Group 2: The top level command text (EJSON or EJSDISABLE)
	 * </ul>
	 */
	private static final Pattern P_COMMANDTOP=Pattern.compile(
			"^[\\p{Space}&&[^\\n]]*\\#\\#(\\#?)(EJSON|EJSDISABLE)[\\p{Space}&&[^\\n]]*$", Pattern.MULTILINE|Pattern.UNIX_LINES);
	
	/**
	 * In normal scanning mode, matches a non-escaped introductory sequence.
	 * The presence of an introductory sequence will put the parser into a
	 * different state unless if the sequence is an escape, in which it just
	 * appends to the literal buffer the unescaped form.
	 * <p>
	 * This pattern matches any of the following:
	 * <ul>
	 * <li>##= at the start of a line
	 * <li>## at the start of a line
	 * <li>#{ anywhere
	 * </ul>
	 * <p>
	 * If group 1 is non empty, then a start of line directive was encountered.
	 * If the group starts with three pound signs, then it is an escape.  If it ends
	 * with an equals, then it is a block intro.
	 * 
	 * <p>
	 * If group 2 is non empty, then this is an interpolation start "#{".  If
	 * it starts with two pound signs, then it is an escape.
	 */
	private static final Pattern P_INTRO=Pattern.compile(
			"(?:^[\\p{Space}&&[^\\n]]*(\\#?\\#\\#\\=?))|(\\#?\\#\\{)", Pattern.MULTILINE|Pattern.UNIX_LINES);
	
	/**
	 * Match all main mode commands, potentially surrounded by spaces.
	 * Group(1) is the matched text.
	 */
	private static final Pattern P_CMD_MAIN=Pattern.compile(
			"\\s*(EJSON|EJSOFF|EJSDISABLE)\\s*");
	
	/**
	 * Match a single line directive start.  Used for seeing if a run
	 * of directives should be considered the same block
	 */
	private static final Pattern P_DIRECTIVE_START=Pattern.compile(
			"\\s*\\#\\#(?!\\#)"
			);
	
	/**
	 * Match a block end sequence (does not match trailing newline)
	 */
	private static final Pattern P_BLOCK_END=Pattern.compile(
			"^[\\p{Space}&&[^\\n]]*\\#\\#\\=[\\p{Space}&&[^\\n]]*$",
			Pattern.MULTILINE|Pattern.UNIX_LINES);
	
	public EjsParser(Events events) {
		this.events=events;
		this.location=new LocationInfo();
	}
	
	/**
	 * Increment the lineNumber by counting the line endings between start (inclusive)
	 * and end (exclusive)
	 * @param start
	 * @param end
	 */
	private void incrementLine(int start, int end) {
		for (int i=start; i<end; i++) {
			if (source.charAt(i)=='\n') {
				lineNumber+=1;
			}
		}
	}
	
	/**
	 * If the position is on a line end character, then advance past it and
	 * increment the line count.  Use after a MULTILINE pattern that matches on "$"
	 * but does not include its terminating line end char.
	 */
	private void chomp() {
		if (position<source.length() && source.charAt(position)=='\n') {
			lineNumber+=1;
			position+=1;
		}
	}
	
	/**
	 * Clear the buffer and set the start and end line numbers to the current line
	 */
	private void resetBuffer() {
		buffer.setLength(0);
		location.lineStart=lineNumber;
		location.sourceStart=position;
	}
	
	/**
	 * Output a literal event and reset the buffer
	 */
	private void dumpBufferAsLiteral() {
		if (buffer.length()>0) {
			location.sourceEnd=position;
			events.handleLiteral(buffer, location);
		}
		resetBuffer();
	}
	
	private void dumpBufferAsBlock() {
		if (buffer.length()>0) {
			location.sourceEnd=position;
			events.handleBlock(buffer, location);
		}
		resetBuffer();
	}
	
	private void dumpBufferAsInterpolation() {
		if (buffer.length()>0) {
			location.sourceEnd=position;
			events.handleInterpolation(buffer, location);
		}
		resetBuffer();
	}
	
	/**
	 * @param start
	 * @return The index of the next line terminator or the end of stream
	 */
	private int findLineEnd(int start) {
		while (start<source.length()) {
			if (source.charAt(start)=='\n') return start;
			start+=1;
		}
		return start;
	}
	
	/**
	 * Initiate a parse of the given CharSequence
	 * @param source
	 */
	public void parse(CharSequence source) {
		this.location.source=source;
		this.source=source;
		this.matcher=P_COMMANDTOP.matcher(source);
		
		this.position=0;
		this.lineNumber=1;
		
		// Reset buffer
		if (buffer==null) buffer=new StringBuilder(source.length());
		
		parseStateTop();
	}

	protected void parseStateTop() {
		resetBuffer();
		
		for (;;) {
			if (position>=source.length()) break;
			
			matcher.usePattern(P_COMMANDTOP);
			if (matcher.find(position)) {
				// Found a sequence
				boolean isEscape=matcher.start(1)<matcher.end(1);
				String command=matcher.group(2);
				if (isEscape) {
					// Is an escape.  Accumulate and repeat
					// Note that the way that the pattern is constructed,
					// the match will never contain a line terminator, so
					// we don't add to lines.  Splice out the escaped character.
					
					// Output stuff before the match
					buffer.append(source, position, matcher.start());
					
					// Output sliced match
					buffer.append(source, matcher.start(), matcher.start(1));
					buffer.append(source, matcher.end(1), matcher.end());
					position=matcher.end();
				} else {
					// Append the bit before the match as literal
					buffer.append(source, position, matcher.start());
					incrementLine(position, matcher.start());
					position=matcher.end();
					dumpBufferAsLiteral();
					
					// Increment line numbers past match
					chomp();	// Pattern does not grab the trailing new line
					
					resetBuffer();	// Catch the line number advancement
					
					// Act differently based on different commands
					if ("EJSDISABLE".equals(command)) {
						// Take all additional text after the command and output
						// as literal
						buffer.append(source, matcher.end(), source.length());
						incrementLine(matcher.end(), source.length());
						break;
					} else if ("EJSON".equals(command)) {
						// Perform main parsing
						parseStateMain();
						resetBuffer();
					} else {
						throw new RuntimeException("Unexpected command in top-level parse state");
					}
				}
			} else {
				// No match.  Remainder of source is literal
				buffer.append(source, position, source.length());
				incrementLine(position, source.length());
				position=source.length();
			}
		}
		
		// Final dump of buffer as a literal
		position=source.length();
		dumpBufferAsLiteral();
	}
	
	/**
	 * Do parsing in the main parse state.  This is done just after an ##EJSON
	 * command (position is on the line ending following the command).  This method
	 * will return when either an EJSOFF command is processed or the end of stream
	 * is encountered.  EJSDISABLE commands are handled internal to this method
	 */
	protected void parseStateMain() {
		resetBuffer();
		
		for (;;) {
			if (position>=source.length()) break;
			matcher.usePattern(P_INTRO);
			if (!matcher.find(position)) break;
			
			// Output all earlier literal stuff
			buffer.append(source, position, matcher.start());
			incrementLine(position, matcher.start());
			position=matcher.start();
			dumpBufferAsLiteral();
			
			// Branch based on line directive or interpolation
			String lineStart=matcher.group(1), interpStart=matcher.group(2);
			if (lineStart!=null && !lineStart.isEmpty()) {
				// It is a single line directive
				if (lineStart.startsWith("###")) {
					// It is a single line escape
					buffer.append(lineStart.substring(1));	// Skip first char and add as literal
					position=matcher.end();
					continue;
				}
				
				// Need to get the full first line to process commands
				if (lineStart.equals("##")) {
					int lineEnd=findLineEnd(matcher.end());
					CharSequence commandCheck=source.subSequence(matcher.end(), lineEnd);
					Matcher commandMatcher=P_CMD_MAIN.matcher(commandCheck);
					if (commandMatcher.matches()) {
						position=lineEnd;
						String command=commandMatcher.group(1);
						// Process as command
						if ("EJSOFF".equals(command)) {
							// Just return to top level
							return;
						} else if ("EJSDISABLE".equals(command)) {
							// Output all remaining as literal and return
							resetBuffer();
							buffer.append(source, position, source.length());
							dumpBufferAsLiteral();
							position=source.length();
							return;
						} else if ("EJSON".equals(command)) {
							// Do nothing.  This is a no-op b/c already on
							continue;
						} else {
							throw new IllegalStateException("Unrecognized main mode command " + command);
						}
					} else {
						// Process as run of single line directives
						parseSingleLineDirectives(lineStart, matcher.end());
					}
				} else if (lineStart.equals("##=")) {
					// Process as delimitted block
					parseDelimetedBlock(lineStart, matcher.end());
				}
			} else if (interpStart!=null && !interpStart.isEmpty()){
				// It is an interpolation
				if (interpStart.startsWith("##")) {
					// Process escape sequence
					buffer.append(interpStart.substring(1));	// Skip first char and add as literal
					position=matcher.end();
					continue;
				}
				
				parseInterpolation(interpStart, matcher.end());
			} else {
				throw new RuntimeException("Match error.  Unexpected parse state.");
			}
		}
	}

	/**
	 * Parse a run of single line directives, leaving the position at the first
	 * character following the last single line directive in the run.
	 * @param startToken The token that started the sequence (##)
	 * @param end The index immediately following the startToken
	 */
	private void parseSingleLineDirectives(String startToken, int start) {
		// Accumulate all directive lines into the buffer for output
		position=start;
		resetBuffer();
		
		for (;;) {
			int lineEnd=findLineEnd(start);
			buffer.append(source, start, lineEnd);
			buffer.append('\n');
			
			// Advance and accumulate line ending
			position=lineEnd;
			chomp();
			
			// See if the next line is a directive (and not a command)
			Matcher directiveMatcher=P_DIRECTIVE_START.matcher(source);
			directiveMatcher.region(position, source.length());
			if (directiveMatcher.lookingAt()) {
				startToken=directiveMatcher.group();
				start=directiveMatcher.end();	
				// Note that this matcher has a region set so need to add the offset
				
				// Make sure it is not a command
				lineEnd=findLineEnd(start);
				Matcher commandMatcher=P_CMD_MAIN.matcher(source.subSequence(start, lineEnd));
				if (commandMatcher.matches()) {
					// Break out and let the main parser handle it
					break;
				} else {
					// Continue and append this next line
					continue;
				}
			} else {
				// Nothing further in the run
				break;
			}
		}
		
		dumpBufferAsBlock();
	}

	/**
	 * Parse a delimitted block.
	 * @param startToken The token that started the block (##=)
	 * @param start The index immediately following the start token
	 */
	private void parseDelimetedBlock(String startToken, int start) {
		resetBuffer();
		matcher.usePattern(P_BLOCK_END);
		int blockEnd;
		if (matcher.find(start)) {
			blockEnd=matcher.start();
			position=matcher.end();
			chomp();
		} else {
			// It is the rest of the stream
			blockEnd=source.length();
			position=source.length();
		}
		
		incrementLine(start, blockEnd);
		buffer.append(source, start, blockEnd);
		dumpBufferAsBlock();
	}

	/**
	 * Parse an interpolation sequence.  startToken is the string that started
	 * the interpolation (#{).  start is the index of the first character after
	 * the start token in the source.  This must set the position to the character
	 * following the interpolation.
	 * @param interpStart
	 * @param end
	 */
	private void parseInterpolation(String startToken, int start) {
		resetBuffer();
		int end=scanForInterpolationEnd(start);
		
		incrementLine(start, end);
		buffer.append(source, start, end);
		dumpBufferAsInterpolation();
	}

	/**
	 * Position the stream after the interpolation end sequence, returning the
	 * index of the end sequence (or end of stream if no end sequence found).
	 * @param start
	 * @return
	 */
	private int scanForInterpolationEnd(int start) {
		int braceDepth=1;
		
		while (start<source.length()) {
			char c=source.charAt(start);
			if (c=='\'' || c=='\"') {
				// Scan through string literal
				while (start<source.length()) {
					char sc=source.charAt(start);
					if (sc==c && source.charAt(start-1)!='\\') {
						// End char
						start+=1;
						break;
					}
					
					start+=1;
				}
			} else {
				// Process a regular character
				if (c=='{') braceDepth+=1;
				if (c=='}') braceDepth-=1;

				if (braceDepth==0) {
					position=start+1;
					return start;
				}

				start+=1;
			}
		}
		
		// End of stream
		position=source.length();
		return position;
	}


	
}
