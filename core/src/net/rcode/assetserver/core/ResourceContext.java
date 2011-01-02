package net.rcode.assetserver.core;

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
	/**
	 * The path component prefix that this configuration applies to.  This is primarily
	 * used for translating global paths to local paths for matching purposes.
	 */
	private String[] appliesToPrefix;
	
	
	
	public ResourceContext(String[] appliesToPrefix) {
		this.appliesToPrefix=appliesToPrefix.clone();
	}
	
	
}
