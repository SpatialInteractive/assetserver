package net.rcode.assetserver.addon;

import net.rcode.assetserver.core.AssetServer;

/**
 * Main descriptive interface that the addon must implement.
 * @author stella
 *
 */
public interface Addon {
	public String getAddonName();
	public void configure(AssetServer server) throws Exception;
}
