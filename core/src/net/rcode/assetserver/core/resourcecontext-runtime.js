/**
 * JavaScript runtime library for initializing resource contexts.
 * Provides an API for configuring the server in a DSL-ish fashion.
 * The runtime environment will always provide the following
 * references:
 * <ul>
 * <li>logger - A Java (SLF4J) logger instance
 * <li>context - The ResourceContext being constructed
 * <li>global - The global scope (different for evaluation and runtime)
 * </ul>
 * 
 * This library provides all other bits of the api.
 */

/**
 * Finds a filter by id or class.  If looking by id,
 * prepend the argument with a '#'.  Raises an error
 * if the filter is not found.  This method takes
 * variable arguments and returns the first valid
 * filter, raising an error if none are found.
 */
function filter(/* names */) {
	var i, name, instance;
	for (i=0; i<arguments.length; i++) {
		name=arguments[i];
		instance=lookupByName(name);
		if (instance) return instance;
	}
	
	// If here, none found
	throw new Error('Could not find filter with search [' +
			Array.prototype.join.call(arguments, ',') + ']');
	
	function lookupByName(name) {
		name=String(name);
		if (name==='') return null;
		if (name[0]==='#') {
			// Lookup by id
		} else if (name[0]==='@') {
			// Lookup by Java class name
		} else {
			// Lookup by generic name
		}
	}
}