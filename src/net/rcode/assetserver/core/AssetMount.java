package net.rcode.assetserver.core;

/**
 * Represents a subtree under an AssetRoot.  This is an abstract path.  See
 * sub-classes for the types of mounts that can be used.
 * 
 * @author stella
 *
 */
public abstract class AssetMount {

	/**
	 * Find an asset
	 * @param mountPath
	 * @return AssetLocator or null if not found
	 */
	public abstract AssetLocator resolve(String mountPath);
}
