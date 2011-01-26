(function() {

/**
 * Pack an html string
 */
function htmlpackString(htmlText, locator, options, sourceName) {
	if (!options) options={};
	
	if (htmlText===null || htmlText===undefined) {
		throw new Error('In call to htmlpackString(...), htmlText cannot be null or undefined');
	}
	var packer=hostobjects.htmlpack.createPacker(String(htmlText));
	if (!locator) {
		packer=packer.selectFirstChild();
	} else {
		packer=packer.selectByAttribute("fragment", String(locator));
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
	if (contents===null || contents===undefined) {
		throw new Error('In call to htmlpack(...), the resource ' + resourceName + ' was not found');
	}
	return htmlpackString(contents, locator, options, resourceName);
};

// Exports
global.htmlpackString=function(htmlText, locator, options) {
	return htmlpackString(htmlText, locator, options, "literal html fragment");
};
global.htmlpack=htmlpack;

})();
