/**
 * serverconfig-runtime.js
 * Laoded into a sub-scope inherited from the scope containing resourcecontext-runtime.js.
 * This sets up the API used to configure the server.
 * The following globals will be available:
 * <ul>
 * <li>context - root ResourceContext
 * <li>server - AssetServer
 * <li>logger
 * </ul>
 */
(function(global) {
// Imports
var core=Packages.net.rcode.assetserver.core,
	ResourceMount=core.ResourceMount,
	File=java.io.File;

/**
 * Mount a physical directory to a logical server path.
 * Example: mount("/static", "web/static")
 * If the physicalDirectory is not absolute, it is made absolute relative
 * to the server config location
 */
global.mount=function(serverPath, physicalDirectory) {
	var physicalFile=new File(String(physicalDirectory));
	
	// Make absolute
	if (!physicalFile.isAbsolute()) {
		physicalFile=new File(server.getConfigDirectory(), physicalDirectory);
	}
	
	// Instantiate the mount
	var resourceMount=new ResourceMount(physicalFile, server);
	
	// And add it
	server.getRoot().add(String(serverPath), resourceMount);
};

/**
 * Load an addon
 */
global.loadAddon=function(name) {
	server.addonManager.load(name);
};

})(this);
