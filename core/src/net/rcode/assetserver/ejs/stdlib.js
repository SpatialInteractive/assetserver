/**
 * Baseline additions to the standard library
 */
(function(global) {

/** Imports **/
var StringEscapeUtils=Packages.org.apache.commons.lang3.StringEscapeUtils;

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
})(global);