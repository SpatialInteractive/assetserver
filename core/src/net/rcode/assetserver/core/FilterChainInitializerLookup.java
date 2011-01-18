package net.rcode.assetserver.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Looks up ResourceFilters by a combination of ids, categories
 * or classes, caching the filter instances.
 * <p>
 * Names have a syntax defined by the first character of the name:
 * <ul>
 * <li>'@' prefix = the remainder of the string is a Java class name
 * <li>'#' prefix = the remainder of the string is the id of the filter
 * <li>any other = the string represents the generic category name
 * </ul>
 * 
 * @author stella
 *
 */
public class FilterChainInitializerLookup {
	/**
	 * Map of aliases to remap names
	 */
	private Map<String, String> nameAliases=new HashMap<String, String>();
	
	/**
	 * Cache of builtin names to initializers
	 */
	private ConcurrentHashMap<String, FilterChainInitializer> instanceCache=new ConcurrentHashMap<String, FilterChainInitializer>();
	
	/**
	 * Adds builtin filters to the alias set.  This is a hack until we
	 * have a better registration scheme.
	 */
	public void addBuiltins() {
		String classPrefix="@net.rcode.assetserver.";
		
		// Ignore
		alias(classPrefix + "core.IgnoreResourceFilter",
				"#std-ignore",
				"ignore");
		
		// Ejs
		alias(classPrefix + "ejs.EjsResourceFilter",
				"#std-ejs",
				"ejs");
		
		// Optimizers
		alias(classPrefix + "optimizer.YuiOptimizeJsResourceFilter",
				"#yui-jsoptimize",
				"jsoptimize");
		alias(classPrefix + "optimizer.YuiOptimizeCssResourceFilter",
				"#yui-cssoptimize",
				"cssoptimize");
		
		// Svg
		//alias(classPrefix + "svg.SvgRenderResourceFilter",
		//		"#std-svgrender",
		//		"svgrender");
	}
	
	/**
	 * Establish one or more aliases
	 * @param rootName
	 * @param aliases
	 */
	public void alias(String rootName, String... aliases) {
		for (String alias: aliases) {
			nameAliases.put(alias, rootName);
		}
	}
	
	/**
	 * Establish a hard binding to a filter.  This is used by addons to register actual
	 * instances instead of aliases.
	 * @param name
	 * @param initializer
	 */
	public void set(String name, FilterChainInitializer initializer) {
		instanceCache.put(name, initializer);
	}
	
	public FilterChainInitializer lookup(String name) {
		// Process aliases
		String aliasedName;
		Set<String> circularDetect=null;
		for (;;) {
			aliasedName=nameAliases.get(name);
			if (aliasedName==null) break;
			if (circularDetect==null) circularDetect=new HashSet<String>();
			if (!circularDetect.add(name)) {
				throw new IllegalStateException("Circular reference on filter alias " + name);
			}
			
			name=aliasedName;
		}
		
		// Check cache
		FilterChainInitializer ret=instanceCache.get(name);
		if (ret!=null) return ret;
		
		// Instantiate and cache
		if (name==null || name.isEmpty()) return null;
		if (!name.startsWith("@")) {
			throw new IllegalStateException("Cannot instantiate non class filter " + name);
		}
		
		String className=name.substring(1);
		ret=instantiate(className);
		instanceCache.put(name, ret);
		
		return ret;
	}

	protected FilterChainInitializer instantiate(String className) {
		// TODO: This is naive handling of classloaders and needs
		// to be updated when plugins are defined
		try {
			ClassLoader cl=getClass().getClassLoader();
			Class<?> clazz=Class.forName(className, true, cl);
			Object ret=clazz.newInstance();
			return (FilterChainInitializer) ret;
		} catch (ClassCastException e) {
			throw new IllegalStateException("Filter defined by " + className + " is not an instanceof FilterChainInitializer");
		} catch (Exception e) {
			throw new RuntimeException("Error instantiating filter class " + className, e);
		}
	}
}
