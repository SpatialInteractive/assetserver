package net.rcode.assetserver.ejs;

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
 * 	##EJSON
 *  ##EJSOFF
 * </pre>
 * Each command must exist on its own line with nothing but whitespace
 * separating it from adjacent lines.  The stream starts in "OFF" mode,
 * whereby everything encountered is considered a literal.  The presense
 * of an ##EJSON command marks the following region (or remainder of the stream)
 * as subject to EJS parsing.
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

}
