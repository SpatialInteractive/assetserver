package net.rcode.assetserver.core;


/**
 * Base class for implementing resource filters.  Filters exist in a chain.
 * Each entry in the chain is an instance of this class and is typically a
 * stateless entity.  All state for the rendering session is maintained by
 * the FilterChain instance that invokes it.
 * 
 * @author stella
 *
 */
public abstract class ResourceFilter implements FilterChainInitializer {
	private String id;
	
	protected ResourceFilter(String id) {
		this.id=id;
	}
	
	public String getId() {
		return id;
	}
	
	/**
	 * Base class implementation adds this to the end of the filters list
	 * of the chain
	 */
	@Override
	public void initializeChain(FilterChain chain) {
		chain.getFilters().addLast(this);
	}
	
	/**
	 * Filters an AssetLocator from a source to a target
	 * @param context
	 * @param source
	 * @return an AssetLocator or null to signal not found
	 * @throws Exception
	 */
	public abstract AssetLocator filter(FilterChain context, AssetLocator source) throws Exception;
}
