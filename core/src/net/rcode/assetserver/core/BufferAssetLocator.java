package net.rcode.assetserver.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * AssetLocator wrapping a buffer with all properties mutable
 * @author stella
 *
 */
public class BufferAssetLocator implements AssetLocator {
	private boolean shouldCache;
	private String contentType;
	private String characterEncoding;
	private String eTag;
	private byte[] buffer;
	
	public void setShouldCache(boolean shouldCache) {
		this.shouldCache = shouldCache;
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}
	
	public void setETag(String eTag) {
		this.eTag = eTag;
	}
	
	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}
	
	@Override
	public boolean shouldCache() {
		return shouldCache;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public String getCharacterEncoding() {
		return characterEncoding;
	}

	@Override
	public String getETag() {
		return eTag;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(buffer==null ? new byte[0]: buffer);
	}

	@Override
	public long getLength() {
		return buffer.length;
	}

}
