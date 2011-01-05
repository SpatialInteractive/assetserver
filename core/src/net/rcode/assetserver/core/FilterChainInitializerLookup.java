package net.rcode.assetserver.core;

import java.util.HashMap;
import java.util.Map;
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
		String prefix="@net.rcode.assetserver.";
		
		// Ejs
		alias(prefix + "ejs.EjsResourceFilter",
				"#std-ejs",
				"ejs");
		
		// Optimizers
		alias(prefix + "optimizer.YuiOptimizeJsResourceFilter",
				"#yui-jsoptimize",
				"jsoptimize");
		alias(prefix + "optimizer.YuiOptimizeCssResourceFilter",
				"#yui-cssoptimize",
				"cssoptimize");
		
		// Svg
		alias(prefix + "svg.SvgRenderResourceFilter",
				"#std-svgrender",
				"svgrender");
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
}
