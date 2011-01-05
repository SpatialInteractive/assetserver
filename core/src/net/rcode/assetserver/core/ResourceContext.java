package net.rcode.assetserver.core;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Each resource is evaluated against a context that defines all rules for resolving
 * the request.  In theory, an EvaluationContext could come from anywhere, but in practice
 * they are constructed hierarchically starting at the root of a mount point, with a new
 * one being instantiated at each point in the directory tree where a .asaccess file is
 * encountered.  Contexts are mutable during construction and are then frozen.  The only
 * way to make a context mutable again is to copy it and modify the copy.  This is
 * how the hierarchy is constructed.  At each level of the tree, a check is made for
 * the .asaccess file and if found, a copy of the previous context is made and used
 * as the basis to evaluate the new access file against.
 * <p>
 * Upon freezing, a hash is taken of the context state and this is used as a scope
 * identifier for an external cache (so that when invalidated, the cache can be flushed).
 * 
 * @author stella
 *
 */
public class ResourceContext {
	private boolean frozen;
	private ResourceContext parent;
	private List<FilterBinding> filters;
	
	/**
	 * Binds a predicate to an initializer which should be invoked if the predicate passes.
	 * @author stella
	 *
	 */
	public static class FilterBinding {
		public final AssetPredicate predicate;
		public final FilterChainInitializer initializer;
		
		public FilterBinding(AssetPredicate predicate, FilterChainInitializer initializer) {
			this.predicate=predicate;
			this.initializer=initializer;
		}
	}
	
	public ResourceContext(ResourceContext parent) {
		this.parent=parent;
		this.filters=new LinkedList<FilterBinding>();
	}
	
	/**
	 * Copies salient properties from the parent such that this child
	 * context represents the same state as the parent but can be
	 * modified independently.  If there is no parent, then this
	 * does nothing.
	 */
	public void importParent() {
		if (parent==null) return;
		
		// Add all bindings from the parent
		filters.addAll(parent.getFilters());
	}
	
	/**
	 * Freeze the context, preventing further modification
	 */
	public void freeze() {
		if (frozen) return;
		
		frozen=true;
		filters=Collections.unmodifiableList(filters);
	}
	
	/**
	 * @return true if this context is frozen
	 */
	public boolean isFrozen() {
		return frozen;
	}
	
	/**
	 * @return reference to the parent context
	 */
	public ResourceContext getParent() {
		return parent;
	}
	
	/**
	 * Get the list of filters.
	 * @return the list of filters, read-only if frozen
	 */
	public List<FilterBinding> getFilters() {
		return filters;
	}
	
}
