package net.rcode.assetserver.cache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import net.rcode.assetserver.core.AssetLocator;
import net.rcode.assetserver.util.MessageDigestBuilder;

/**
 * An entry in the cache.  This also implements AssetLocator so it can be returned
 * directly to respond to a client.
 * 
 * @author stella
 *
 */
public class CacheEntry implements Serializable, AssetLocator {
	private static final long serialVersionUID=CachingResourceHandler.GLOBAL_SERIAL_VERSION_UID;
	
	private CacheIdentity identity;
	private CacheDependency[] dependencies;
	private String contentType;
	private String characterEncoding;
	private byte[] contents;
	private volatile transient String etag;
	
	protected CacheEntry() { }
	public CacheEntry(CacheIdentity identity, CacheDependency[] dependencies, String contentType, String characterEncoding, byte[] contents) {
		this.identity=identity;
		this.dependencies=dependencies;
		this.contentType=contentType;
		this.characterEncoding=characterEncoding;
		this.contents=contents;
	}
	
	@Override
	public String getETag() {
		String value=etag;
		if (value==null && contents!=null) {
			MessageDigestBuilder b=new MessageDigestBuilder("MD5");
			b.append(contents);
			value=b.getValueAsHex();
			etag=value;
		}
		return value;
	}
	
	public CacheIdentity getIdentity() {
		return identity;
	}
	
	/**
	 * 
	 * @return true if all dependencies are valid
	 */
	public boolean isValid() {
		if (dependencies==null) return true;
		for (CacheDependency cd: dependencies) {
			if (cd!=null && !cd.isValid()) return false;
		}
		
		return true;
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
	public InputStream getInputStream() throws IOException {
		if (contents==null) return new ByteArrayInputStream(new byte[0]);
		else return new ByteArrayInputStream(contents);
	}
	
	@Override
	public long getLength() {
		if (contents==null) return 0;
		return contents.length;
	}
}