package net.rcode.assetserver.cache;

import java.io.File;

import net.rcode.assetserver.core.AssetLocator;
import net.rcode.assetserver.core.AssetPath;
import net.rcode.assetserver.core.ResourceHandler;
import net.rcode.assetserver.core.ResourceMount;


/**
 * Base class for resources composed dynamically of an arbitrary number
 * of other resources where the results should be persistently cached.
 * <p>
 * The persistent cache is by default located at #{owner.location}/.cache
 * or the location specified by setCacheLocation(...).  The .cache directory
 * consists of files named by the consistent hash (MD5) of the internal
 * CacheIdentity object.  The syntax of each file is:
 * <blockquote>
 * 		{serialUID}-#{cacheIdentity.getHashKey()}-{sequence[1..n]}
 * </blockquote>
 * Most entries will have a sequence number of 1 but in the event of collision,
 * this index can be incremented.  The actual cacheIdentity is stored with the
 * contents and compared on get().
 * <p>
 * This instance assumes that in the event of a cache miss, it is relatively
 * inexpensive to regenerate the contents.  Therefore, it doesn't try *very hard*
 * to avoid duplicate content generation for race conditions, collisions or
 * errors, generally prefering just to drop the suspect entries and start over.
 * <p>
 * Each file is a serialized form of a CacheEntry object which contains embedded
 * references to the paths and timestamps of resources that the entry depends on.
 * If any of the dependents change, the CacheEntry is assumed to be invalid and
 * subject to regeneration.
 * <p>
 * The implementation is safe for multi-threaded or multi-process use.  The overall
 * serial UID is part of the consistent hash, so multiple versions of the code should
 * be capable of co-existing against the same location.
 * <p>
 * The implementation is consistent with respect to any external interference with the
 * cache during runtime.  For example, externally clearing or deleting the directory is
 * perfectly valid.
 * 
 * @author stella
 *
 */
public abstract class CachingResourceHandler implements ResourceHandler {
	public static final long GLOBAL_SERIAL_VERSION_UID=1l;
	
	/**
	 * The override cacheLocation.  If null, a default is used.
	 */
	private File cacheLocation;
	
	@Override
	public final AssetLocator accessResource(ResourceMount owner,
			AssetPath assetPath, File physicalResource) throws Exception {
		File actualCacheLocation=cacheLocation;
		if (actualCacheLocation==null) {
			// Use a default
			actualCacheLocation=new File(owner.getLocation(), ".cache");
		}
		
		Cache cache=new Cache(actualCacheLocation);
		CacheIdentity identity=new CacheIdentity(getClass().getName(),
				assetPath.getMountPoint(), assetPath.getPath(),
				"");	// TODO: Add environment options redux
		
		CacheEntry entry=cache.lookup(identity);
		if (entry!=null && entry.isValid()) return entry;
		
		// No hit.  Generate.
		entry=generateResource(identity, owner, assetPath, physicalResource);
		if (entry==null) return null;
		
		// Convert to a CacheEntry
		cache.store(entry);
		return entry;	// Return the actual cache entry
	}

	/**
	 * Subclasses should override this method to generate the resource.
	 * @param owner
	 * @param assetPath
	 * @param physicalResource
	 * @return
	 * @throws Exception
	 */
	protected abstract CacheEntry generateResource(CacheIdentity identity, 
			ResourceMount owner,
			AssetPath assetPath, File physicalResource) throws Exception;
	
	public File getCacheLocation() {
		return cacheLocation;
	}
	public void setCacheLocation(File cacheLocation) {
		this.cacheLocation = cacheLocation;
	}
}
