package net.rcode.assetserver.core;

/**
 * Class representing a match of a given incoming path against an AssetMount
 * with the path broken out into components.
 * @author stella
 *
 */
public class AssetPath {
	/**
	 * The mount that this path is bound to
	 */
	public AssetMount mount;
	
	/**
	 * The mount point (path prefix) of the owning mount.  This will always include the
	 * leading, but not the trailing slash of the prefix unless if it is the root path,
	 * in which case, it just the empty string.
	 */
	public String mountPoint;
	
	/**
	 * The path within the given AssetMount.  This will always have a leading slash.
	 */
	public String mountPath;
	
	public AssetPath(AssetMount mount, String mountPoint, String mountPath) {
		this.mount=mount;
		this.mountPoint=mountPoint;
		this.mountPath=mountPath;
	}
}
