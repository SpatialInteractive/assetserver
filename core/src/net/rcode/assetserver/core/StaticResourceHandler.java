package net.rcode.assetserver.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
		final boolean isText=mimeMapping.isTextualMimeType(mimeType);
		
		return new AssetLocator() {

			@Override
			public String getContentType() {
				return mimeType;
			}

			@Override
			public String getCharacterEncoding() {
				if (isText) return defaultEncoding;
				else return null;
			}

			@Override
			public String getETag() {
				return "F:" + physicalResource.lastModified() + ":" + physicalResource.length();
			}
			
			@Override
			public void writeTo(OutputStream output) throws IOException {
				InputStream input=new FileInputStream(physicalResource);
				byte[] buffer=new byte[4096];
				
				try {
					for (;;) {
						int r=input.read(buffer);
						if (r<0) break;
						if (r==0) continue;
						output.write(buffer, 0, r);
					}
				} finally {
					input.close();
				}
			}
			
			@Override
			public long getLength() {
				return physicalResource.length();
			}
			
		};
	}
	
}
