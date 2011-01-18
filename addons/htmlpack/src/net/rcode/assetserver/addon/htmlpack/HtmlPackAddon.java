package net.rcode.assetserver.addon.htmlpack;

import net.rcode.assetserver.addon.Addon;
import net.rcode.assetserver.core.AssetServer;
import net.rcode.assetserver.ejs.EjsRuntime;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Main addon class for HtmlPack addon.
 * <p>
 * Extends the JavaScript runtime in the following ways:
 * <ul>
 * <li>Adds a hostobjects.htmlpack object, which is an instance of the native class HtmlPackHostObject
 * <li>Evaluates the htmlpack-api.js source file to create the public api, exporting the global htmlpack
 * </ul>
 * 
 * @author stella
 *
 */
public class HtmlPackAddon implements Addon {

	@Override
	public String getAddonName() {
		return "htmlpack";
	}

	@Override
	public void configure(AssetServer server) {
		// Add the htmlpack host object
		EjsRuntime jsRuntime = server.getJavascriptRuntime();
		Scriptable hostObjects=jsRuntime.getHostObjects();
		ScriptableObject.putProperty(hostObjects, "htmlpack", jsRuntime.javaToJs(new HtmlPackHostObject()));
		
		jsRuntime.loadLibrary(getClass(), "htmlpack-api.js");
	}

}
