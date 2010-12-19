package net.rcode.assetserver.cache;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import net.rcode.assetserver.core.AssetLocator;

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
	
	protected CacheEntry() { }
	public CacheEntry(CacheIdentity identity, CacheDependency[] dependencies, String contentType, String characterEncoding, byte[] contents) {
		this.identity=identity;
		this.dependencies=dependencies;
		this.contentType=contentType;
		this.characterEncoding=characterEncoding;
		this.contents=contents;
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
	public void writeTo(OutputStream out) throws IOException {
		if (contents!=null) out.write(contents);
	}
}