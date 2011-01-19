(function() {

/**
 * Pack an html string
 */
function htmlpackString(htmlText, locator, options, sourceName) {
	if (!options) options={};
	
	var packer=hostobjects.htmlpack.createPacker(htmlText);
	if (!locator) {
		packer=packer.selectFirstChild();
	} else {
		packer=packer.selectById(String(locator));
	}
	
	if (!packer) {
		throw new Error('Cannot find element "' + locator + '" in html from ' + sourceName);
	}
	
	return String(packer.pack());
};

/**
 * Pack a resource.
 */
function htmlpack(resourceName, locator, options) {
	if (!options) options={};
	
	var contents=read(resourceName, options);
	return htmlpackString(contents, locator, options, resourceName);
};

// Exports
global.htmlpackString=function(htmlText, locator, options) {
	return htmlpackString(htmlText, locator, options, "literal html fragment");
};
global.htmlpack=htmlpack;

})();
