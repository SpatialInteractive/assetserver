package net.rcode.cphelp;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Provides access to a ClassLoader hierarchy specified at application initialization time.
 * @author stella
 *
 */
public class LoaderLookup {
	private static final String LOADER_RESOURCE="META-INF/loader.properties";
	private static LoaderLookup INSTANCE;
	
	private Map<String, ClassLoader> loaders=new HashMap<String, ClassLoader>();
	private Properties loaderProperties;
	private Set<String> loadingNames=new HashSet<String>();
	
	private static Properties loadProperties(String resourceName) {
		Properties props=new Properties();
		Enumeration<URL> resources;
		try {
			resources=Main.class.getClassLoader().getResources(resourceName);
		} catch (IOException e) {
			System.err.println("Error enumerating classpath resources for " + resourceName + ": " + e.getMessage());
			return props;
		}
		while (resources.hasMoreElements()) {
			URL resource=resources.nextElement();
			try {
				Reader in=new InputStreamReader(resource.openStream(), "UTF-8");
				try {
					props.load(in);
				} finally {
					in.close();
				}
			} catch (IOException e) {
				// Ignore
				System.err.println("Error loading classpath resource " + resourceName + ": " + e.getMessage());
			}
		}
		
		return props;
	}

	public static synchronized LoaderLookup getInstance() {
		if (INSTANCE==null) {
			INSTANCE=new LoaderLookup(loadProperties(LOADER_RESOURCE));
		}
		return INSTANCE;
	}
	
	public LoaderLookup(Properties loaderProperties) {
		this.loaderProperties=loaderProperties;
		
		ClassLoader system=ClassLoader.getSystemClassLoader();
		loaders.put("system", system);
		loaders.put("this", LoaderLookup.class.getClassLoader());
		loaders.put("bootstrap", system.getParent());
	}
	
	public Properties getLoaderProperties() {
		return loaderProperties;
	}
	
	/**
	 * Get a loader by name
	 * @param name
	 * @return loader or null
	 */
	public synchronized ClassLoader lookup(String name) {
		ClassLoader cl=loaders.get(name);
		if (cl==null) {
			// Look in properties
			String prefix="loader." + name;
			String clType=loaderProperties.getProperty(prefix);
			if (clType==null) return null;
			
			if (!loadingNames.add(name)) {
				throw new IllegalStateException("Recursive definition of classloader " + name);
			}
			try {
				cl=instantiateClassLoader(name, loaderProperties, prefix, clType);
			} finally {
				loadingNames.remove(name);
			}
			loaders.put(name, cl);
		}
		
		return cl;
	}
	
	/**
	 * Instantiate a class loader by type, prefix and props
	 * @param props
	 * @param prefix
	 * @param clType
	 * @return ClassLoader
	 */
	private ClassLoader instantiateClassLoader(String name, Properties props,
			String prefix, String clType) {
		// First resolve the parent
		String parentName=props.getProperty(prefix + ".parent", "this");
		ClassLoader parent=lookup(parentName);
		if (parent==null) {
			throw new IllegalStateException("Could not find parent loader " + parentName + " while defining sub-loader " + name);
		}
		
		// Branch based on type
		try {
			if ("url".equalsIgnoreCase(clType)) {
				// Create a URLClassLoader
				return instantiateUrlClassLoader(props, prefix, parent);
			} else if ("bundled".equalsIgnoreCase(clType)) {
				// Create a bundled classloader
				return instantiateBundledClassLoader(props, prefix, parent);
			}
		} catch (Exception e) {
			throw new IllegalStateException("Error defining loader " + name + ": " + e.getMessage());
		}
		
		throw new IllegalStateException("Unknown loader type '" + clType + "' for loader " + name);
	}

	private ClassLoader instantiateBundledClassLoader(Properties props,
			String prefix, ClassLoader parent) {
		List<String> prefixes=new ArrayList<String>();
		for (int i=0; ; i++) {
			String loadPrefix=props.getProperty(prefix + '.' + i);
			if (loadPrefix==null) break;
			prefixes.add(loadPrefix);
		}
		
		return new BundledClassLoader(prefixes.toArray(new String[prefixes.size()]), parent);
	}

	private ClassLoader instantiateUrlClassLoader(Properties props,
			String prefix, ClassLoader parent) throws IOException {
		List<URL> urls=new ArrayList<URL>();
		for (int i=0; ; i++) {
			String urlString=props.getProperty(prefix + '.' + i);
			if (urlString==null) break;
			urls.add(new URL(urlString));
		}
		
		return new URLClassLoader(urls.toArray(new URL[urls.size()]), parent);
	}

	/**
	 * Add a ClassLoader
	 * @param name
	 * @param loader
	 */
	public synchronized void define(String name, ClassLoader loader) {
		loaders.put(name, loader);
	}
	
	public static boolean hasClass(String className, ClassLoader cl) {
		try {
			Class.forName(className, false, cl);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
