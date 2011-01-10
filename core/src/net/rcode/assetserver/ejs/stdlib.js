/**
 * Baseline additions to the standard library
 */
(function(global) {

/** Imports **/
var StringEscapeUtils=Packages.org.apache.commons.lang3.StringEscapeUtils,
	assetserver=Packages.net.rcode.assetserver,
	PathUtil=assetserver.util.PathUtil,
	IOUtil=assetserver.util.IOUtil;

/** Augment the String.prototype with methods for escaping in various ways **/
String.prototype.toJs=function() {
	return String(StringEscapeUtils.escapeEcmaScript(this));
}
String.prototype.toJava=function() {
	return String(StringEscapeUtils.escapeJava(this));
}
String.prototype.toHtml=function() {
	return String(StringEscapeUtils.escapeHtml4(this));
}
String.prototype.toXml=function() {
	return String(StringEscapeUtils.escapeXml(this));
}

/** Global IO functions **/

/**
 * Read a named resource relative to the currently evaluating resource.  Unless
 * if overriden, the resource is expected to be a text file in UTF-8 encoding.
 * <p>
 * This method properly manages the cache such that all physical dependencies
 * processed to generate the resource are tracked as part of the invoking resource.
 * 
 * @return the resource content as a String or null
 */
global.read=function(resourceName, options) {
	if (!options) options={};
	
	// Normalize the path
	var relativeTo=PathUtil.dirname(runtime.filterChain.assetPath.fullPath),
		normalizedPath=PathUtil.normalizePath(relativeTo, resourceName);

	// Get the locator
	var locator=runtime.server.root.resolve(normalizedPath);
	if (!locator) return null;
	
	// Handle encoding
	var encoding=options.encoding || locator.characterEncoding || 'UTF-8';
	return String(IOUtil.decodeBufferToString(locator, encoding));
};

/**
 * Write to the current resource.  If text===null or text===undefined, then
 * nothing is written.  Otherwise, the value of String(text) is written.
 */
global.write=function(text) {
	if (text===null || text===undefined) return;
	runtime.rawWrite(text);	// The appendable writer invokes a String(...) function
};

/**
 * Similar to read but outputs the contents of the resource directly into the
 * current resource.  Unlike read, if the resource is not found, it is an error.
 */
global.include=function(resourceName, options) {
	var contents=global.read(resourceName, options);
	if (contents===null) {
		throw new Error('In call to include(...), ' + resourceName + ' could not be found.');
	}
	
	runtime.rawWrite(contents);
};

/**
 * Return the full absolute path of the resource requested by the client
 */
global.requestedPath=function() {
	return runtime.requestContext.activeFilterChains.get(0).assetPath.fullPath;
};

/**
 * Return the full absolute path to the current resource
 */
global.thisPath=function() {
	return runtime.filterChain.assetPath.fullPath;
};

/**
 * Convert a path interpreted relative to this resource to a path
 * interpreted relative to the root resource of the request (ie.
 * the path that the client thinks it's requesting).
 * <p>
 * This is a useful mechanism for HTML and CSS files which can contain
 * paths relative to the containing resource.
 * 
 * TODO: There are cache implications to doing this that need to
 * be fixed.  The cache identity needs to include a redux of the include
 * path
 */
global.clientRelative=function(path) {
	var fromPath=PathUtil.dirname(global.thisPath()),
		toPath=PathUtil.dirname(global.requestedPath());
	
	return PathUtil.translateRelative(fromPath, toPath, path);
};

})(global);
