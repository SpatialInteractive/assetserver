package net.rcode.assetserver.core;

import java.util.ArrayList;
import java.util.List;

import net.rcode.assetserver.util.NamePattern;

/**
 * Contains a list of pairs of AssetPredicate and ResourceFilter.
 * Used to populate a FilterChain based on the resource being processed.
 * 
 * @author stella
 *
 */
public class FilterSelector {
	private static class Pair {
		public AssetPredicate predicate;
		public ResourceFilter filter;
	}
	
	private List<Pair> filters=new ArrayList<Pair>();
	
	public FilterSelector() {
	}
	
	public void add(AssetPredicate predicate, ResourceFilter filter) {
		Pair pair=new Pair();
		pair.predicate=predicate;
		pair.filter=filter;
		filters.add(pair);
	}
	
	public void add(String namePattern, ResourceFilter filter) {
		add(new NamePattern(namePattern), filter);
	}
	
	public void add(NamePattern namePattern, ResourceFilter filter) {
		add(new NamePatternPredicate(namePattern), filter);
	}
	
	public void build(AssetPath assetPath, FilterChain chain) {
		for (Pair pair: filters) {
			if (pair.predicate.matches(assetPath))
				chain.getFilters().addLast(pair.filter);
		}
	}
}
