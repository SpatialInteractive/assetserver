package net.rcode.assetserver.core;

import java.io.File;

/**
 * Handle resources of a given type.  This serves as a factory for producing
 * AssetLocator instances that can be used to access the contents.
 * @author stella
 *
 */
public interface ResourceHandler {
	/**
	 * Return an AssetLocator.
	 * @param owner
	 * @param assetPath
	 * @param physicalResource
	 * @return the asset locator for the path or null if it should be "not found"
	 * @throws Exception
	 */
	public AssetLocator accessResource(ResourceMount owner, AssetPath assetPath, File physicalResource)
		throws Exception;
}
