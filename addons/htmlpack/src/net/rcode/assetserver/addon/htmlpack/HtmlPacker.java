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
 * Given a Document Element, builds a packed representation.
 * @author stella
 *
 */
public class HtmlPacker {
	private Element rootElement;
	
	public HtmlPacker(Element rootElement) {
		this.rootElement=rootElement;
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
