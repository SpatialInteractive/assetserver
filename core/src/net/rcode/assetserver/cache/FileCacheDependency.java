package net.rcode.assetserver.cache;

import java.io.File;
import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * Declare a dependency on a File.  Stores a snapshot of the file's full path,
 * modified time and size.  If any of these change, the dependency is considered
 * invalid.  This class can also be used to track dependencies on files that do
 * not exist.  It will invalidate if they end up existing at a later time.
 * 
 * @author stella
 *
 */
public class FileCacheDependency extends CacheDependency implements Serializable {
	private static final long serialVersionUID=CachingResourceHandler.GLOBAL_SERIAL_VERSION_UID;
	
	private String dependentPath;
	private boolean exists;
	private long modified;
	private long length;
	
	protected FileCacheDependency() { }
	public FileCacheDependency(File dependentFile) {
		this.dependentPath=dependentFile.getAbsolutePath();
		this.exists=dependentFile.exists();
		this.modified=dependentFile.lastModified();
		this.length=dependentFile.length();
	}
	
	@Override
	public boolean isValid() {
		File currentFile=new File(dependentPath);
		return new EqualsBuilder()
			.append(exists, currentFile.exists())
			.append(modified, currentFile.lastModified())
			.append(length, currentFile.length())
			.isEquals();
	}
	
	@Override
	public int hashCode() {
		return dependentPath.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj==null) return false;
		if (!(obj instanceof FileCacheDependency)) return false;
		return ((FileCacheDependency)obj).dependentPath.equals(dependentPath);
	}
}