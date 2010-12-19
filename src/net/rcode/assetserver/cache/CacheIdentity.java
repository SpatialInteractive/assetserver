package net.rcode.assetserver.cache;

import java.io.Serializable;

import net.rcode.assetserver.util.MessageDigestBuilder;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class CacheIdentity implements Serializable {
	private static final long serialVersionUID=CachingResourceHandler.GLOBAL_SERIAL_VERSION_UID;
	
	private String handlerClassName;
	private String mountPoint;
	private String path;
	private String environmentRedux;
	
	protected CacheIdentity() { }
	public CacheIdentity(String handlerClassName, String mountPoint, String path, String environmentRedux) {
		this.handlerClassName=handlerClassName;
		this.mountPoint=mountPoint;
		this.path=path;
		this.environmentRedux=environmentRedux;
	}
	
	/**
	 * @return {serialVersionUID}-{consistent hash}
	 */
	public String getExternalName() {
		MessageDigestBuilder b=new MessageDigestBuilder("MD5");
		b.append(handlerClassName);
		b.append(mountPoint);
		b.append(path);
		b.append(environmentRedux);
		
		return serialVersionUID + "-" + b.getValueAsHex();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder()
			.append(handlerClassName)
			.append(mountPoint)
			.append(path)
			.append(environmentRedux)
			.toHashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other==null) return false;
		if (other==this) return true;
		if (other.getClass()!=this.getClass()) return false;
		
		CacheIdentity rhs=(CacheIdentity) other;
		return new EqualsBuilder()
			.append(handlerClassName, rhs.handlerClassName)
			.append(mountPoint, rhs.mountPoint)
			.append(path, rhs.path)
			.append(environmentRedux, rhs.environmentRedux)
			.isEquals();
	}
}