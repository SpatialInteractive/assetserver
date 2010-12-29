package net.rcode.assetserver.core;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;

import net.rcode.assetserver.cache.CacheDependency;

/**
 * A recursive, push-style filter mechanism for translating content.
 * <p>
 * A FilterChain is assembled to produce a sequence of translations
 * and can be applied to arbitrary content streams.
 * 
 * @author stella
 *
 */
public class FilterChain {
	private Set<CacheDependency> dependencies=new HashSet<CacheDependency>();
	private AssetLocator rootLocator;
	private AssetPath assetPath;
	private AssetServer server;
	
	private LinkedList<ResourceFilter> filters=new LinkedList<ResourceFilter>();
	private AssetLocator current;
	private File rootFile;
	
	public FilterChain(AssetServer server, AssetPath assetPath, AssetLocator rootLocator, File rootFile) {
		this.server=server;
		this.assetPath=assetPath;
		this.rootLocator=rootLocator;
		this.current=rootLocator;
		this.rootFile=rootFile;
	}
	
	/**
	 * Process all filters until the filters list is empty.  The list of filters
	 * can be modified at each step.  Filter iteration also stops if a filter
	 * returns null.
	 * @throws Exception 
	 */
	public void processFilters() throws Exception {
		while (current!=null && !filters.isEmpty()) {
			ResourceFilter filter=filters.removeFirst();
			current=filter.filter(this, current);
		}
	}
	
	/**
	 * Get the current asset locator
	 * @return current or null if a "not found" condition
	 */
	public AssetLocator getCurrent() {
		return current;
	}
	
	/**
	 * Call to modify the current AssetLocator outside of a filter context
	 * @param current
	 */
	public void setCurrent(AssetLocator current) {
		this.current = current;
	}
	
	/**
	 * Set of all dependent objects that went into rendering this filter chain.
	 * Add items to this set to ensure proper cache invalidation
	 * @return set of dependencies
	 */
	public Set<CacheDependency> getDependencies() {
		return dependencies;
	}
	
	/**
	 * The root AssetLocator that begins the chain
	 * @return root locator
	 */
	public AssetLocator getRootLocator() {
		return rootLocator;
	}
	
	/**
	 * @return The AssetPath for the top level requested resource
	 */
	public AssetPath getAssetPath() {
		return assetPath;
	}
	
	/**
	 * @return the owning server instance
	 */
	public AssetServer getServer() {
		return server;
	}
	
	/**
	 * Filters are processed from head to tail on this list
	 * @return the filter list
	 */
	public LinkedList<ResourceFilter> getFilters() {
		return filters;
	}
	
	/**
	 * @return the file representing the root resource
	 */
	public File getRootFile() {
		return rootFile;
	}
	
	/**
	 * Finds a filter by id
	 * @param id
	 * @return a ListIterator positioned just after the filter with the given id  
	 */
	public ListIterator<ResourceFilter> findFilterById(String id) {
		ListIterator<ResourceFilter> iter=filters.listIterator();
		while (iter.hasNext()) {
			ResourceFilter f=iter.next();
			if (id.equals(f.getId())) {
				iter.previous();
				break;
			}
		}
		return iter;
	}
}
