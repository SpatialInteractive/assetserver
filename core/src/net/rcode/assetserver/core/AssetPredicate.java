package net.rcode.assetserver.core;

/**
 * Defines a predicate function for matching against an asset
 * @author stella
 *
 */
public interface AssetPredicate {
	/**
	 * 
	 * @param assetPath
	 * @return true if the predicate passes
	 */
	public boolean matches(AssetPath assetPath);
}
