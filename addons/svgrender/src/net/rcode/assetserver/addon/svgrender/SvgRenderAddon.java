package net.rcode.assetserver.addon.svgrender;

import net.rcode.assetserver.addon.Addon;
import net.rcode.assetserver.core.AssetServer;

public class SvgRenderAddon implements Addon {

	@Override
	public String getAddonName() {
		return "svgrender";
	}

	@Override
	public void configure(AssetServer server) {
		String classLocator="@" + SvgRenderResourceFilter.class.getName();
		SvgRenderResourceFilter filter=new SvgRenderResourceFilter();
		
		// Register the filters
		server.getFilterLookup().set(classLocator, filter);
		server.getFilterLookup().alias(classLocator, "#svgsalamander-svgrender", "svgrender");
	}
}
