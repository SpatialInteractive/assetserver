package net.rcode.assetserver.core;

import java.io.File;

/**
 * Resource handler that just serves up static files.  Typically added as the
 * last match rule for "*"
 * @author stella
 *
 */
public class StaticResourceHandler implements ResourceHandler {
	private String defaultEncoding="UTF-8";
	
	public StaticResourceHandler() {
	}
	
	public String getDefaultEncoding() {
		return defaultEncoding;
	}
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}
	
	@Override
	public AssetLocator accessResource(ResourceMount owner,
			AssetPath assetPath, final File physicalResource) throws Exception {
		MimeMapping mimeMapping=owner.getServer().getMimeMapping();
		final String mimeType=mimeMapping.lookup(assetPath.getBaseName());
		
		FileAssetLocator al=new FileAssetLocator(physicalResource);
		al.setContentType(mimeType);
		if (mimeMapping.isTextualMimeType(mimeType)) al.setCharacterEncoding(defaultEncoding);
		return al;
	}
	
}
