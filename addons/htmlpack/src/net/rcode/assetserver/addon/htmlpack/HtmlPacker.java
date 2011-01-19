package net.rcode.assetserver.addon.htmlpack;

import java.io.StringWriter;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringEscapeUtils;
import org.htmlcleaner.ConfigFileTagProvider;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.ITagInfoProvider;
import org.htmlcleaner.TagNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

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
public class HtmlPacker implements Cloneable {
	private static ITagInfoProvider htmlcleanerTagInfo;
	
	private static final Pattern CLASSSPLIT=Pattern.compile("\\s+");
	
	private Element rootElement;
	private String idAttribute="id";
	private String cssClassAttribute="class";
	
	public HtmlPacker(Element rootElement) {
		this.rootElement=rootElement;
	}
	
	public HtmlPacker copyWith(Element newRoot) {
		try {
			HtmlPacker ret=(HtmlPacker) clone();
			ret.rootElement=newRoot;
			return ret;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @return an html packer focused on the first child element
	 */
	public HtmlPacker selectFirstChild() {
		if (rootElement==null) return this;
		for (Node child=rootElement.getFirstChild(); child!=null; child=child.getNextSibling()) {
			if (child.getNodeType()==Node.ELEMENT_NODE) {
				return copyWith((Element) child);
			}
		}
		
		// Not found
		return copyWith(null);
	}
	
	/**
	 * Pack the current element, returning a CharSequence of the result
	 * @return result
	 */
	public CharSequence pack() {
		if (rootElement==null) return "[]";
		
		StringBuilder out=new StringBuilder();
		packElement(rootElement, out);
		
		return out;
	}
	
	private void packElement(Element element, StringBuilder out) {
		out.append('[');
		out.append(stringLiteral(elementName(element)));
		
		// Now collect the attributes
		NamedNodeMap attrs=element.getAttributes();
		for (int i=0; i<attrs.getLength(); i++) {
			Attr attr=(Attr) attrs.item(i);
			// Skip namespaced attrs
			if (attr.getNamespaceURI()!=null) continue;
			
			String name=attr.getName();
			// If one of our special names, ignore
			if (name.equalsIgnoreCase(idAttribute) || name.equalsIgnoreCase(cssClassAttribute))
				continue;
			
			// Otherwise, output it
			out.append(',');
			out.append(stringLiteral('@' + name));
			out.append(',');
			out.append(stringLiteral(attr.getValue()));
		}
		
		// And finally the content
		boolean normalizeSpacing=shouldNormalize(element);
		boolean trailingSpace=false;
		for (Node child=element.getFirstChild(); child!=null; child=child.getNextSibling()) {
			switch (child.getNodeType()) {
			case Node.ELEMENT_NODE:
				// Process recursively
				if (trailingSpace) {
					out.append(",0");	// Emit a single space shortcut
					trailingSpace=false;
				}
				out.append(',');
				packElement((Element) child, out);
				break;
				
			case Node.TEXT_NODE:
			case Node.CDATA_SECTION_NODE:
				// Process text
				// If the text node only contains whitespace, then we deal with this separately by setting
				// a flag so that it gets appended before the next child or the close
				String textValue=child.getNodeValue();
				if (!normalizeSpacing) {
					// The element wants spacing preserved, so output it verbatim
					out.append(',');
					out.append(stringLiteral(child.getNodeValue()));
				} else {
					textValue=normalizeSpacing(textValue);
					if (isWhitespace(textValue)) {
						trailingSpace=true;
					} else {
						// Write it out
						if (trailingSpace) {
							// If trailing space, tack one onto the beginning
							textValue=' ' + textValue;
							trailingSpace=false;
						} else if (textValue.startsWith("@")) {
							// Escape leading at sign
							textValue='@' + textValue;
						}
						out.append(',');
						out.append(stringLiteral(textValue));
					}
				}
				break;
				
			case Node.COMMENT_NODE:
			case Node.PROCESSING_INSTRUCTION_NODE:
				// Explicit ignore
				continue;
				
			default:
				throw new IllegalStateException("Unexpected node type " + child.getNodeType());
			}
		}
		
		// Handle any trailing space
		if (trailingSpace) {
			out.append(",0");
			trailingSpace=false;
		}
		
		out.append(']');
	}
	
	private static final Pattern SPACE_PATTERN=Pattern.compile("\\s+", Pattern.MULTILINE);
	private boolean isWhitespace(String textValue) {
		return SPACE_PATTERN.matcher(textValue).matches();
	}
	
	private String normalizeSpacing(String textValue) {
		return textValue;
	}

	private boolean shouldNormalize(Element element) {
		return true;
	}

	private CharSequence stringLiteral(CharSequence in) {
		return '\'' + StringEscapeUtils.ESCAPE_ECMASCRIPT.translate(in) + '\'';
	}
	
	private CharSequence elementName(Element element) {
		StringBuilder name=new StringBuilder();
		name.append(element.getTagName());
		
		// Process id attribute
		if (idAttribute!=null) {
			Attr attr=element.getAttributeNode(idAttribute);
			if (attr!=null) {
				name.append('#');
				name.append(attr.getValue());
			}
		}
		
		// Process css class
		if (cssClassAttribute!=null) {
			Attr attr=element.getAttributeNode(cssClassAttribute);
			if (attr!=null) {
				// Split the css class
				String[] classNames=CLASSSPLIT.split(attr.getValue());
				for (String className: classNames) {
					if (className.isEmpty()) continue;
					name.append('.');
					name.append(className);
				}
			}
		}
		
		return name;
	}
	
	/**
	 * Return an HtmlPacker focused on the document root element
	 * @param source
	 * @return
	 * @throws Exception
	 */
	/*
	public static HtmlPacker parse(InputSource source) throws Exception {
		XMLReader parser=new Parser();
		parser.setFeature(Parser.namespacesFeature, false);
		parser.setFeature(Parser.namespacePrefixesFeature, false);
		parser.setFeature(Parser.CDATAElementsFeature, true);
		
		Transformer transformer=TransformerFactory.newInstance().newTransformer();
		
		DOMResult result=new DOMResult();
		transformer.transform(new SAXSource(parser, source), result);
		Node node=result.getNode();
		Element element;
		if (node instanceof Element) element=(Element) node;
		else if (node instanceof Document) {
			element=((Document)node).getDocumentElement();
			
			// Find the body.  tagsoup always produces a full html tree
			NodeList nl=element.getElementsByTagName("body");
			if (nl.getLength()>0) {
				element=(Element) nl.item(0);
			}
		} else {
			throw new IllegalStateException();
		}
		
		return new HtmlPacker(element);
	}
	*/
	public static void initSettings() {
		if (htmlcleanerTagInfo==null) {
			htmlcleanerTagInfo=new ConfigFileTagProvider(HtmlPacker.class.getResource("htmlcleaner-tags.xml"));
		}
	}
	
	public static HtmlPacker parse(InputSource source) throws Exception {
		initSettings();
		
		HtmlCleaner cleaner=new HtmlCleaner(htmlcleanerTagInfo);
		TagNode rootNode;
		if (source.getCharacterStream()!=null) {
			rootNode=cleaner.clean(source.getCharacterStream());
		} else {
			throw new IllegalStateException("InputSource not supported");
		}
		
		DomSerializer s=new DomSerializer(cleaner.getProperties());
		Node node=s.createDOM(rootNode);
		dumpDom(node);
		
		Element element;
		if (node instanceof Element) element=(Element) node;
		else if (node instanceof Document) {
			element=((Document)node).getDocumentElement();
			
			// Find the body.  tagsoup always produces a full html tree
			NodeList nl=element.getElementsByTagName("body");
			if (nl.getLength()>0) {
				element=(Element) nl.item(0);
			}
		} else {
			throw new IllegalStateException();
		}

		return new HtmlPacker(element);
	}

	private static void dumpDom(Node domDoc) throws Exception {
		StringWriter writer=new StringWriter();
		StreamResult result=new StreamResult(writer);
		DOMSource source=new DOMSource(domDoc);
		TransformerFactory.newInstance().newTransformer().transform(source, result);
		
		String xml=writer.toString();
		System.err.println(xml);
	}
}
