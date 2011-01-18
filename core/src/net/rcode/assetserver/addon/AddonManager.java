package net.rcode.assetserver.addon;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import net.rcode.assetserver.core.AssetServer;

/**
 * A server can own an AddonManager which is used to manage loadable addons.
 * Addins can extend the server in various ways, such as:
 * <ul>
 * <li>Adding library components to the EJS JavaScript runtime
 * <li>Adding filters
 * <li>Configuring root filters
 * </ul>
 * 
 * By default, the server does not load any addons, but an instance of this class
 * can be used to scan for and activate them.
 * <p>
 * Addons are self contained in their own zip file which contains an asaddon.properties
 * file which describes it.  The properties file has the following entries:
 * <ul>
 * <li>class: The name of a class implementing the Addon interface.  If multiple
 * addons are represented in the archive, then multiple classes can be given separated by 
 * commas (required)
 * <li>classpath: comma separated list of prefixes within the archive that should be added
 * to the addon classpath.  If not present, then the classpath will just be the root of
 * the archive.  If specified, then this implicit root will not be included. (optional)
 * </ul>
 * 
 * <p>
 * When an addon is activated, it can be done by its "simple name".  This name will result
 * in a file name of the form "{simplename}.asaddon".  The first occurence of this
 * file on the search path will be used.
 * 
 * @author stella
 *
 */
public class AddonManager {
	private static final Pattern COMMA_SPLIT=Pattern.compile("\\s*\\,\\s*");
	
	/**
	 * List of paths to be searched for addons
	 */
	private List<File> searchPath=new ArrayList<File>();
	
	/**
	 * Map of loaded addons
	 */
	private Map<String, AddonEntry> addons=new HashMap<String, AddonEntry>();
	
	/**
	 * The owning server instance
	 */
	private AssetServer server;
	
	private static class AddonEntry {
		public Properties properties;
		public URL jarUrl;
		public ClassLoader classLoader;
		public Addon addon;
	}
	
	public AddonManager(AssetServer server) {
		this.server=server;
	}
	
	public List<File> getSearchPath() {
		return searchPath;
	}
	
	public Collection<String> getNames() {
		return Collections.unmodifiableCollection(addons.keySet());
	}
	
	protected File findByName(String name) {
		// If name is a path, then treat it as a file
		if (name.indexOf('/')>=0 || name.indexOf('\\')>=0) {
			File ret=new File(name);
			if (!ret.isFile()) return null;
			return ret;
		}
		
		// Search path
		for (File path: searchPath) {
			File test=new File(path, name + ".asaddon");
			if (test.isFile()) return test;
		}
		
		return null;
	}
	
	/**
	 * Load an addon by name.  If already loaded, then just return
	 * @param name
	 * @throws Throwable on failure
	 */
	public void load(String name) throws RuntimeException {
		try {
			loadUnchecked(name);
		} catch (Throwable t) {
			throw new RuntimeException("Error loading addon '" + name + "'", t);
		}
	}
	
	/**
	 * Loads the addon but does minimal exception handling
	 * @param name
	 * @throws Throwable
	 */
	private void loadUnchecked(String name) throws Throwable {
		if (addons.containsKey(name)) return;
		
		File addonFile=findByName(name);
		if (addonFile==null) {
			throw new RuntimeException("Cannot find addon with name " + name);
		}
		
		AddonEntry entry=new AddonEntry();
		
		// Construct the path to the jar
		URI jarFileUri=addonFile.toURI();
		entry.jarUrl=new URL("jar:" + jarFileUri.toString() + "!/");
		
		// Load the properties
		entry.properties=new Properties();
		URL propUrl=new URL(entry.jarUrl, "/asaddon.properties");
		InputStream propIn;
		try {
			propIn=propUrl.openStream();
		} catch (IOException e) {
			throw new RuntimeException("Cannot load addon " + name + " from " + entry.jarUrl + " because no asaddon.properties file was found.");
		}
		try {
			entry.properties.load(propIn);
		} finally {
			propIn.close();
		}
		
		// Construct the classloader
		ClassLoader parent=getClass().getClassLoader();
		String[] classPrefixes;
		String classpath=entry.properties.getProperty("classpath", "/");
		classPrefixes=COMMA_SPLIT.split(classpath);
		entry.classLoader=new AddonClassLoader(parent, entry.jarUrl, classPrefixes);
		
		// Load the addon
		String className=entry.properties.getProperty("class", "Addon");
		Class addonClass=Class.forName(className, true, entry.classLoader);
		entry.addon=(Addon) addonClass.newInstance();
		
		// At this point, we're loaded enough that we want to register
		addons.put(name, entry);
		
		// And initialize
		entry.addon.configure(server);
	}
}
