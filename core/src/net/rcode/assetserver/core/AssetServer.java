package net.rcode.assetserver.core;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rcode.assetserver.cache.Cache;
import net.rcode.assetserver.ejs.EjsResourceHandler;
import net.rcode.assetserver.ejs.EjsRuntime;

/**
 * Sets up an assetserver instance.  This class is used at initialization time
 * and produces a Runtime instance.
 * 
 * @author stella
 *
 */
public class AssetServer {
	public static final String CONFIG_NAME="asconfig.js";
	public static final Logger logger=LoggerFactory.getLogger("assetserver");
	
	private EjsRuntime javascriptRuntime;
	private File configDirectory;
	private File configFile;
	
	private AssetRoot root;
	private MimeMapping mimeMapping;
	private File sharedCacheLocation;
	private Cache sharedCache;
	
	private ServerConfig config;
	
	public AssetServer(File location) throws IllegalArgumentException, IOException {
		this.config=new ServerConfig();
		
		this.javascriptRuntime=new EjsRuntime();
		initializeJavaScriptRuntime();
		
		mimeMapping=new MimeMapping();
		mimeMapping.loadDefaults();
		
		setupLocation(location);
		setSharedCacheLocation(new File(configDirectory, ".cache"));
		
		root=new AssetRoot(this);
	}
	
	public Logger getLogger() {
		return logger;
	}
	
	/**
	 * Perform a simple setup.
	 * @throws IOException 
	 */
	public void setupSimple() throws IOException {
		logger.info("Setting up server defaults for location " + configDirectory);
		ResourceMount rootMount=new ResourceMount(configDirectory, this);
		root.add("/", rootMount);
		EjsResourceHandler ejsHandler=new EjsResourceHandler();
		rootMount.addHandler("*", new StaticResourceHandler());
		rootMount.addHandler("*.html", ejsHandler);
		rootMount.addHandler("*.js", ejsHandler);
		rootMount.addHandler("*.css", ejsHandler);
	}
	
	/**
	 * The location of the shared cache or null if no caching.  The default will be the
	 * ".cache" directory under the config directory.
	 * 
	 * @return current shared cache location
	 */
	public File getSharedCacheLocation() {
		return sharedCacheLocation;
	}
	
	/**
	 * Get the global config
	 * @return global config
	 */
	public ServerConfig getConfig() {
		return config;
	}
	
	/**
	 * (Re)set the shared cache location.  Resets the sharedCache field.
	 * @param sharedCacheLocation
	 */
	public void setSharedCacheLocation(File sharedCacheLocation) {
		this.sharedCacheLocation = sharedCacheLocation;
		if (sharedCacheLocation==null) {
			this.sharedCache=null;
		} else {
			this.sharedCache=new Cache(sharedCacheLocation);
		}
	}
	
	/**
	 * @return The shared cache or null if no cache setup
	 */
	public Cache getSharedCache() {
		return sharedCache;
	}
	
	/**
	 * Get the shared runtime for executing script
	 * @return shared runtime
	 */
	public EjsRuntime getJavascriptRuntime() {
		return javascriptRuntime;
	}
	
	public AssetRoot getRoot() {
		return root;
	}
	
	/**
	 * @return The directory containing the config file
	 */
	public File getConfigDirectory() {
		return configDirectory;
	}
	
	/**
	 * @return The config file (within configDirectory).  This file may not exist.
	 */
	public File getConfigFile() {
		return configFile;
	}
	
	public MimeMapping getMimeMapping() {
		return mimeMapping;
	}
	
	private void setupLocation(File location) throws IOException {
		location=location.getCanonicalFile();
		if (location.isDirectory()) {
			// Config file is within the directory
			configFile=new File(location, CONFIG_NAME);
			configDirectory=location;
		} else {
			// Config file is passed in
			configFile=location;
			configDirectory=configFile.getParentFile();
		}
	}

	private void initializeJavaScriptRuntime() {
		javascriptRuntime.loadLibraryStd();
	}
}
