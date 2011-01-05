package net.rcode.assetserver.core;

/**
 * Auxillary interface that initializes the FilterChain, presumably
 * with a filter.  All ResourceFilter instances implement this interface
 * and will add themselves to the filter chain when invoked.  This
 * interface can be used to implement more of a decorator pattern by
 * things that are not subclasses of ResourceFilter.
 * 
 * @author stella
 *
 */
public interface FilterChainInitializer {

	/**
	 * Take appropriate action against the chain, presumably adding
	 * filter(s).
	 * @param chain
	 */
	public void initializeChain(FilterChain chain);
}
