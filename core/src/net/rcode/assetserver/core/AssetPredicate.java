package net.rcode.assetserver.core;

/**
 * Defines a predicate function for matching against an asset
 * @author stella
 *
 */
public interface AssetPredicate {
	/**
	 * Return true or false based on whether the predicate matches
	 * @param assetPath
	 * @return true if the predicate passes
	 */
	public boolean matches(AssetPath assetPath);
}
