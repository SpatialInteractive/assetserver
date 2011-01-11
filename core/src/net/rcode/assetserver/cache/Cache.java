package net.rcode.assetserver.cache;

public interface Cache {

	/**
	 * Find a CacheEntry by identity.  Will only return a matching CacheEntry
	 * if it precisely matches the identity.
	 * <p>
	 * Because this instance maintains no memory cache, calling this method
	 * repeatedly will return different instances.
	 * 
	 * @param identity
	 * @return CacheEntry matched or null
	 */
	public CacheEntry lookup(CacheIdentity identity);

	/**
	 * Store an entry to the cache.  This raises no exceptions on failure.
	 * @param entry
	 */
	public void store(CacheEntry entry);
	
	/**
	 * Clear the cache
	 */
	public void clear();
}