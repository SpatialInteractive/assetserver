package net.rcode.assetserver.core;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

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
	 * @throws IOException 
	 */
	public abstract AssetLocator resolve(AssetPath path) throws Exception;
	
	/**
	 * @return true if calling the stat method of this instance has any
	 * meaning
	 */
	public boolean canStat() {
		return false;
	}
	
	/**
	 * If canStat(), then this method should return a ResourceStat for a path
	 * or null if not found.  Base class implementation just returns null.
	 * @param path
	 * @return resource stat or null
	 * @throws Exception
	 */
	public ResourceStat stat(AssetPath path) throws Exception {
		return null;
	}
	
	/**
	 * Optionally returns a list of child resources for the given path.  If the path
	 * is not a directory, then this method has no meaning and should return an
	 * empty collection.
	 * 
	 * @param parentPath
	 * @return children
	 * @throws Exception
	 */
	public Collection<ResourceStat> listChildren(AssetPath parentPath) throws Exception {
		return Collections.emptySet();
	}
}
