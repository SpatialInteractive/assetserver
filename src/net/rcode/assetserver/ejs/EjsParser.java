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
		private int lineStart;
		private int sourceStart;
		private int sourceEnd;
		
		public LocationInfo() {
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
			"^\\s*\\#\\#(\\#?)(EJSON|EJSDISABLE)\\s*$", Pattern.MULTILINE|Pattern.UNIX_LINES);
	
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
			"(?:^\\s*(\\#?\\#\\#\\=?))|(\\#?\\#\\{)", Pattern.MULTILINE|Pattern.UNIX_LINES);
	
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
	
	/**
	 * Initiate a parse of the given CharSequence
	 * @param source
	 */
	public void parse(CharSequence source) {
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
					buffer.append(source, matcher.start(), matcher.start(1));
					buffer.append(source, matcher.end(1), matcher.end());
					position=matcher.end();
				} else {
					// Append the bit before the match as literal
					buffer.append(source, position, matcher.start());
					position=matcher.end();
					dumpBufferAsLiteral();
					
					// Increment line numbers past match
					incrementLine(position, matcher.start());
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
		// TODO Auto-generated method stub
		
	}


	
}
