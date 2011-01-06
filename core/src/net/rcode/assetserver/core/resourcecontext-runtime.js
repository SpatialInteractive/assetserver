/**
 * JavaScript runtime library for initializing resource contexts.
 * Provides an API for configuring the server in a DSL-ish fashion.
 * The runtime environment will always provide the following
 * references:
 * <ul>
 * <li>logger - A Java (SLF4J) logger instance
 * <li>context - The ResourceContext being constructed
 * </ul>
 * 
 * This library provides all other bits of the api.
 */
(function(global) {
// Imports
var core=Packages.net.rcode.assetserver.core,
	ResourceContext=core.ResourceContext,
	AssetPredicate=core.AssetPredicate,
	FilterChainInitializer=core.FilterChainInitializer,
	FilterBinding=ResourceContext.FilterBinding,
	PatternPredicateFactory=core.PatternPredicateFactory;
	
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
		instance=context.lookupFilterInitializer(name);
		if (instance) return instance;
	}
	
	// If here, none found
	throw new Error('Could not find filter with search [' +
			Array.prototype.join.call(arguments, ',') + ']');
}

// Utilities
function instantiateFilter(spec) {
	var type=typeof spec;
	if (type === 'string') {
		return filter(spec);
	} else if (type === 'object') {
		if (spec instanceof Array) {
			return filter.apply(null, spec);
		} else if (spec instanceof FilterChainInitializer) {
			return spec;
		}
	} else if (type === 'function') {
		// TODO: Need to provide JavaScript wrapper
		throw new Error('Function filters not yet implemented');
	}
	
	throw new Error('Unrecognized filter: ' + spec);
}

function instantiatePattern(spec) {
	var type=typeof spec;
	if (type === 'string') {
		// Treat as a pattern
		return PatternPredicateFactory.build(spec);
	} else if (type === 'object') {
		if (spec instanceof AssetPredicate) return spec;
	} else if (type === 'function') {
		// TODO: Need to support JavaScript wrapper
		throw new Error('Pattern functions not yet implemented');
	}
	
	throw new Error('Unrecognized pattern spec: ' + spec);
}

// Methods of filter
filter.on=function(/* patterns..., filter */) {
	if (arguments.length===0) return;

	var filterSpec=arguments[arguments.length-1], i,
		patternSpec,
		filter,
		predicaate,
		binding;
	
	filter=instantiateFilter(filterSpec);
	
	for (i=0; i<(arguments.length-1); i++) {
		patternSpec=arguments[i];
		predicate=instantiatePattern(patternSpec);
		binding=new FilterBinding(predicate, filter);
		context.filters.add(binding);
	}
};

// Exports
global.filter=filter;

})(this);
