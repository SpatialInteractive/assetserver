package net.rcode.assetserver.core;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages a root ResourceContext and a cache of child contexts mapped by key.
 * Child contexts are maintained as soft references relying on the ability to
 * regenerate them on demand.  Note that since contexts typically form a chain
 * to the root, collection will only occur at the leaves and proceed rootward
 * as the references expire. (TODO: references not currently implemented)
 * <p>
 * This class does not actually manage the creation the contexts but it will
 * maintain contexts created by an outside agent (@see ResourceMount).
 * <p>
 * Lookup operations are typically lightly synchronized (just by way of the interal
 * controls of a ConcurrentHashMap) but mutate operations have explicit synchronization.
 * 
 * @author stella
 *
 */
public class ResourceContextManager {
	private ResourceContext rootContext;
	private ResourceContextBuilder builder;
	
	private ConcurrentHashMap<Object, ResourceContext> childCache=new ConcurrentHashMap<Object, ResourceContext>();
	
	public ResourceContextManager(ResourceContext rootContext, ResourceContextBuilder builder) {
		this.rootContext=rootContext;
		this.builder=builder;
	}
	
	public ResourceContext getRootContext() {
		return rootContext;
	}
	
	public ResourceContextBuilder getBuilder() {
		return builder;
	}
	
	/**
	 * Lookup a ResourceContext by an opaque key (must properly implement disjoint equals and hashCode)
	 * @param key
	 * @return ResourceContext or null
	 */
	public ResourceContext lookup(Object key) {
		return childCache.get(key);
	}
	
	/**
	 * Put a ResourceContext into the cache.
	 * @param key
	 * @param context
	 */
	public synchronized void put(Object key, ResourceContext context) {
		childCache.put(key, context);
	}
}
