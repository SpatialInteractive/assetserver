package net.rcode.assetserver.cache;

/**
 * Dependency class.  Sub-classes must be serializable.
 * @author stella
 *
 */
public abstract class CacheDependency {
	/**
	 * @return true if the dependency is still valid (up to date)
	 */
	public abstract boolean isValid();
}