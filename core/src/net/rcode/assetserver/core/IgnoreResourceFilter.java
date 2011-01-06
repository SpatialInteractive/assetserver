package net.rcode.assetserver.core;

/**
 * Simple filter that suppresses the resource, effectively
 * returning a 404
 * 
 * @author stella
 *
 */
public class IgnoreResourceFilter extends ResourceFilter {

	protected IgnoreResourceFilter() {
		super("ignore");
	}

	@Override
	public AssetLocator filter(FilterChain context, AssetLocator source)
			throws Exception {
		return null;
	}

}
