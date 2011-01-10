package net.rcode.assetserver.core;

import java.util.ArrayList;
import java.util.List;

import net.rcode.assetserver.cache.CacheDependency;

/**
 * Provides overall state for a request accessible by everything on the request
 * thread.  Top-level invokers should setup a RequestContext and bind it to
 * the thread.
 * 
 * @author stella
 *
 */
public class RequestContext {
	private static ThreadLocal<RequestContext> INSTANCE=new ThreadLocal<RequestContext>();
	
	/**
	 * Get the thread local request context
	 * @return instance
	 * @throws IllegalStateException if no context set
	 */
	public static RequestContext getInstance() throws IllegalStateException {
		RequestContext instance=INSTANCE.get();
		if (instance==null) {
			throw new IllegalStateException("No RequestContext bound");
		}
		return instance;
	}
	
	public static RequestContext enter() {
		RequestContext current=INSTANCE.get();
		if (current!=null) {
			current.refCount+=1;
			return current;
		}
		
		current=new RequestContext();
		current.refCount=1;
		INSTANCE.set(current);
		return current;
	}
	
	public static void exit() {
		RequestContext current=INSTANCE.get();
		if (current==null) {
			throw new IllegalStateException("Unbalanced RequestContext.exit()");
		}
		
		if (--current.refCount == 0) {
			INSTANCE.set(null);
		}
	}
	
	private int refCount;
	private List<FilterChain> activeFilterChains=new ArrayList<FilterChain>();
	
	/**
	 * Resource resolution typically involves maintaining a stack of FilterChain
	 * instances which define the context for evaluating the current resource.
	 * This list represents the stack (0=oldest, n-1=current)
	 * @return list of active filters
	 */
	public List<FilterChain> getActiveFilterChains() {
		return activeFilterChains;
	}
	
	/**
	 * @return the current filter chain
	 * @throws IllegalStateException if no current
	 */
	public FilterChain getCurrentFilterChain() {
		try {
			return activeFilterChains.get(activeFilterChains.size()-1);
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalStateException("No filter chains");
		}
	}
	
	/**
	 * Add a FilterChain to the stack
	 */
	public void pushActiveFilterChain(FilterChain fc) {
		activeFilterChains.add(fc);
	}
	
	/**
	 * Pop the most current filter chain
	 * @return popped instance
	 * @throws IllegalStateException if no current
	 */
	public FilterChain popActiveFilterChain() {
		try {
			return activeFilterChains.remove(activeFilterChains.size()-1);
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalStateException("Mismatched popActiveFilterChain()");
		}
	}
	
	/**
	 * Adds a CacheDependency to all active FilterChain instances
	 * @param dependency
	 */
	public void addDependency(CacheDependency dependency) {
		for (FilterChain fc: activeFilterChains) {
			fc.getDependencies().add(dependency);
		}
	}
}
