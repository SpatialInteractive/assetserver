package net.rcode.assetserver.core;

import java.io.File;
import java.io.IOException;

/**
 * Serves assets off of a directory on the file system.  This also serves
 * as a baseclass for asset mounts that process the results to generate
 * the output.
 * <p>
 * This class also imposes a syntax on paths, allowing an argument list.  The
 * last path component can have a syntax like:
 * <pre>
 *    /cdn/loader$profile=mobile$.js
 * </pre>
 * If this pattern is found in the last component, then the filename is rewritten
 * to loader.js and the parameter string is passed down to the resource handler for
 * the type.
 * <p>
 * Each component of the path is separated by a forward slash and the contents url decoded
 * to match a file on the file system.  A number of characters are illegal in file
 * components and will cause the path to be rejected.  The following characters are illegal:
 * <pre>
 * 	/\:
 * </pre>
 * In addition, no path component can be ".." or "." to avoid directory traversal attacks.
 * <p>
 * Prior to matching a resource, the file is located on the filesystem and compared to
 * its canonical path in order to work around case insesntive file systems.
 * 
 * @author stella
 *
 */
public class ResourceMount extends AssetMount {
	/**
	 * The root location of this mount (canonicalized)
	 */
	private File location;
	
	public ResourceMount(File location) throws IOException {
		this.location=location.getCanonicalFile();
	}
	
	public File getLocation() {
		return location;
	}
	
	@Override
	public AssetLocator resolve(String mountPath) {
		return null;
	}
}
