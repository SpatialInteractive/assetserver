package net.rcode.assetserver.core;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Maintain mime mappings.  Defaults are read from default-mime-mappings.xml
 * @author stella
 *
 */
public class MimeMapping {
	/**
	 * Map of extension to mime type
	 */
	private Map<String, String> mappings=new HashMap<String, String>(2000);
	
	/**
	 * Set of mime types that are textual.  All "text/*" types are automatically.  This
	 * is just for others.
	 */
	private Set<String> textMimeTypes=new HashSet<String>();
	
	/**
	 * Lookup the mimetype by filename extension
	 * @param fileName
	 * @return mime type or null
	 */
	public String lookup(String fileName) {
		if (fileName==null) return null;
		int dotPos=fileName.lastIndexOf('.');
		if (dotPos<0) return null;
		String extension=fileName.substring(dotPos+1).toLowerCase();
		String mimeType=mappings.get(extension);
		return mimeType;
	}
	
	public boolean isTextualMimeType(String mimeType) {
		if (mimeType==null) return false;
		if (mimeType.startsWith("text/")) return true;
		return textMimeTypes.contains(mimeType);
	}
	
	public void addMapping(String extension, String mimeType) {
		mappings.put(extension.toLowerCase(), mimeType);
	}
	
	public void addExplicitTextMimeType(String mimeType) {
		textMimeTypes.add(mimeType);
	}
	
	public void loadDefaults() {
		InputStream is=MimeMapping.class.getResourceAsStream("default-mime-mappings.xml");
		try {
			loadXmlFile(is);
		} finally {
			try {
				is.close();
			} catch (Exception e) { }
		}
	}
	
	public void loadXmlFile(InputStream input) {
		try {
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
			NodeList nl=doc.getElementsByTagName("mime-mapping");
			for (int i=0; i<nl.getLength(); i++) {
				Element mmElt=(Element) nl.item(i);
				NodeList extNl=mmElt.getElementsByTagName("extension");
				NodeList mtNl=mmElt.getElementsByTagName("mime-type");
				if (extNl.getLength()!=1 && mtNl.getLength()!=1) continue;
				
				String extension=extNl.item(0).getTextContent();
				String mimeType=mtNl.item(0).getTextContent();
				if (extension==null || mimeType==null) continue;
				
				addMapping(extension.trim(), mimeType.trim());
				
				if (mmElt.getElementsByTagName("text").getLength()!=0) {
					addExplicitTextMimeType(mimeType.trim());
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Error loading mime mappings", e);
		}
	}
}
