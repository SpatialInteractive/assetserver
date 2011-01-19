package net.rcode.assetserver.addon.htmlpack;

import java.io.Reader;
import java.io.StringReader;

/**
 * An instance of this class is added to the shared EJS scope as
 * hostobjects.htmlpack.
 * 
 * @author stella
 *
 */
public class HtmlPackHostObject {
	
	/**
	 * Creates an HtmlPacker instance from the given htmlText
	 * @param htmlText
	 * @return packer
	 * @throws Exception
	 */
	public HtmlPacker createPacker(String htmlText) throws Exception {
		Reader reader=new StringReader(htmlText);
		return HtmlPacker.parse(reader);
	}
}
