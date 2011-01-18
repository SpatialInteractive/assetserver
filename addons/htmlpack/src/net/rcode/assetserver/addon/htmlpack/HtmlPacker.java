package net.rcode.assetserver.addon.htmlpack;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;

import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * Given a Document Element, builds a packed representation.  The representation is syntactically a nested
 * JavaScript array where each array represents the following items in sequence:
 * <ul>
 * <li>encoded tag name, id and class names
 * <li>alternated attribute name/values as '@name', 'value'
 * <li>arbitrary children
 * </ul>
 * 
 * <p>
 * The encoded tag name must always be a string and is always the first element of an array.  Syntactically, it
 * is
 * <pre>
 *    tagname(#id)?(.classname)*
 * </pre>
 * In this way, the two most common attributes (id, class) in html are represented as part of the name, reducing
 * the size.  Note that this means that this assumes that the class and id attributes follow syntax rules strictly.
 * <p>
 * Following the tagname element, there can be an arbitrary number of alternated name/value array elements,
 * representing all attributes (other than id and class).  The name of each must start with "@" to distinguish
 * it from the arbitrary children that follows.
 * <p>
 * Arbitrary children can follow the attributes (if any) and can be any of the following types:
 * <ul>
 * <li>string - the child is treated as a literal text node.  the packer will normalize spacing
 * for regular html elements, so this may just be a ' ' for a run of spaces and newlines.  If the literal
 * text begins with an at sign (@), then it must be escaped as a double at sign (@@) in order to avoid
 * ambiguity with attribute pairs.
 * <li>array - the child is treated as an element decoded from the given array
 * <li>number - numbers are used to represent compact escapes.  currently, only the number 0 is used
 * and represents a text node with a single space (save two characters for the common case of space
 * as separator)
 * </ul>
 * 
 * The array can be included literally in a JavaScript file or in a double quoted string (").  All quotes
 * will be escaped in the packed form but only single quotes (') will be used for embedded literals.  As
 * such, it is safe to include the generated packed form in a double quoted string with no further
 * escaping.
 * 
 * @author stella
 *
 */
public class HtmlPacker {
	private Element rootElement;
	
	public HtmlPacker(Element rootElement) {
		this.rootElement=rootElement;
	}
	
	/**
	 * Pack the current element, returning a CharSequence of the result
	 * @return result
	 */
	public CharSequence pack() {
		return null;
	}
	
	/**
	 * Return an HtmlPacker focused on the document root element
	 * @param source
	 * @return
	 * @throws Exception
	 */
	public static HtmlPacker parse(InputSource source) throws Exception {
		XMLReader parser=new Parser();
		parser.setFeature(Parser.namespacesFeature, false);
		parser.setFeature(Parser.namespacePrefixesFeature, false);
		
		Transformer transformer=TransformerFactory.newInstance().newTransformer();
		
		DOMResult result=new DOMResult();
		transformer.transform(new SAXSource(parser, source), result);
		Node node=result.getNode();
		
		return new HtmlPacker((Element) node);
	}
}
