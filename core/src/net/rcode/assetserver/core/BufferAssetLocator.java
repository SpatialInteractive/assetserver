package net.rcode.assetserver.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.rcode.assetserver.util.BufferAccessor;

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
	private BufferAccessor delegate;

	public BufferAssetLocator(BufferAccessor delegate) {
		this.delegate=delegate;
	}
	
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

	public InputStream openInput() throws IOException {
		return delegate.openInput();
	}

	public byte[] getBytes() throws IOException {
		return delegate.getBytes();
	}

	public void writeTo(OutputStream out) throws IOException {
		delegate.writeTo(out);
	}

	public long length() {
		return delegate.length();
	}

}
