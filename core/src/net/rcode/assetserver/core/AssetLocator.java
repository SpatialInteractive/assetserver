package net.rcode.assetserver.core;

import net.rcode.assetserver.util.BufferAccessor;

/**
 * The result of resolving a path.  Used to access an asset.
 * @author stella
 *
 */
public interface AssetLocator extends BufferAccessor {
	/**
	 * @return true if the locator should be cached (ie. it is expensive to regenerate)
	 */
	public boolean shouldCache();
	
	/**
	 * @return The asset's content type or null if unknown
	 */
	public String getContentType();
	
	/**
	 * @return The asset's character encoding or null if not applicable
	 */
	public String getCharacterEncoding();
	
	/**
	 * @return An HTTP compliant ETag value for the resource (or null)
	 */
	public String getETag();
	
}
