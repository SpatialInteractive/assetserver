package net.rcode.assetserver.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class PatternPredicateFactoryTest {
	private static AssetPath makePath(String path) {
		return new AssetPath(null, "", path);
	}
	
	@Test
	public void testNameLiteral() {
		AssetPredicate pred=PatternPredicateFactory.build("somefile.txt");
		assertEquals("NameLiteralPredicate(somefile.txt)", pred.toString());
		
		assertTrue(pred.matches(makePath("/abc/somefile.txt")));
		assertFalse(pred.matches(makePath("/abc/otherfile.txt")));
		assertFalse(pred.matches(makePath("/abc/somefile.txt/other")));
	}
	
	@Test
	public void testNamePattern() {
		AssetPredicate pred=PatternPredicateFactory.build("*.js");
		assertEquals("NamePatternPredicate(*.js)", pred.toString());
		
		assertTrue(pred.matches(makePath("/abc/mylib.js")));
		assertFalse(pred.matches(makePath("/abc/other.xml")));
		assertFalse(pred.matches(makePath("/abc/mylib.js/other")));
	}
	
	@Test
	public void testPathPattern() {
		AssetPredicate pred=PatternPredicateFactory.build("/**/js/*.js");
		assertEquals("PathPatternPredicate([**, js, *.js])", pred.toString());
		
		assertTrue(pred.matches(makePath("/mysite/static/js/jquery.js")));
		assertFalse(pred.matches(makePath("/mysite/static/css/style.css")));
		assertFalse(pred.matches(makePath("/mysite/static/css/style.js")));
	}
	
	
	@Test
	public void testPathPrefix() {
		AssetPredicate pred=PatternPredicateFactory.build("/mysite/static/**/*.js");
		assertEquals("PrefixPatternPredicate(/mysite/static,[**, *.js])", pred.toString());
		
		assertTrue(pred.matches(makePath("/mysite/static/js/lib/jquery.js")));
		assertFalse(pred.matches(makePath("/othersite/static/js/lib/jquery.js")));
		assertFalse(pred.matches(makePath("/mysite/static/css/style.css")));
		assertFalse(pred.matches(makePath("/mysite/staticjs/lib/jquery.js")));
		
	}
}
