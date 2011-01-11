package net.rcode.assetserver.cache;

/**
 * Implements Cache but does nothing
 * @author stella
 *
 */
public class NullCache implements Cache {

	@Override
	public CacheEntry lookup(CacheIdentity identity) {
		return null;
	}

	@Override
	public void store(CacheEntry entry) {
	}

	@Override
	public void clear() {
	}

}
