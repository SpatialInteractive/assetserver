package net.rcode.assetserver.core;

/**
 * Callaback for apis that produce lists of assets
 * @author stella
 *
 */
public interface ScanCallback {
	/**
	 * Handle an actual asset.  The path and the locaator are given to the callback.
	 * In many cases, this means that the asset has already been pre-processed.
	 * 
	 * @param path
	 * @param locator
	 * @return true if this asset should have its associated resources enumerated
	 * @throws Exception
	 */
	public boolean handleAsset(AssetPath path) throws Exception;
	
	/**
	 * Handle an AssetPath representing a directory.  This can be used to
	 * further query for more assets.
	 * 
	 * @param path
	 * @return true if the scanner should process the children of the directory
	 * @throws Exception
	 */
	public boolean handleDirectory(AssetPath path) throws Exception;
}
