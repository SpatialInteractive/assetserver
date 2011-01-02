package net.rcode.assetserver.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases around PathPattern matching
 * @author stella
 *
 */
public class PathPatternTest {
	private void assertMatches(String pattern, String path) {
		PathPattern pp=new PathPattern(pattern);
		boolean result=pp.matches(path);
		assertTrue("'" + path + "' matches '" + pattern + "'", result);
	}

	private void assertNotMatches(String pattern, String path) {
		PathPattern pp=new PathPattern(pattern);
		boolean result=pp.matches(path);
		assertFalse("'" + path + "' does not match '" + pattern + "'", result);
	}

	@Test
	public void testPositiveLiterals() {
		assertMatches("/abc/def.txt", "/abc/def.txt");
		assertMatches("/", "/");
		assertMatches("/abc", "/abc");
	}
	
	@Test
	public void testNegativeLiterals() {
		assertNotMatches("/abc/def.txt", "/abc/123.txt");
		assertNotMatches("/", "/abc.txt");
		assertNotMatches("/abc.txt", "/");
	}
	
	@Test
	public void testNameWildPositive() {
		assertMatches("/abc/123.*", "/abc/123.txt");
		assertMatches("/abc/123.*", "/abc/123.txt.js");
		assertMatches("/abc/123.*", "/abc/123.");
		assertMatches("/*/123.txt", "/abc/123.txt");
	}

	@Test
	public void testNameWildNegative() {
		assertNotMatches("/abc*/123.*", "/ab/123.txt");
		assertNotMatches("/abc/123.*", "/abc/456.txt");
		assertNotMatches("/*/123.txt", "/123.txt");
	}

	@Test
	public void testDeepStarPositive() {
		assertMatches("/**", "/");
		assertMatches("/**", "/abc/123");
		assertMatches("/abc/**", "/abc/123");
		assertMatches("/**/123/456.txt", "/123/456.txt");
		assertMatches("/**/123/456.txt", "/abc/def/123/456.txt");
		assertMatches("/**/*.js", "/abc/def/script.js");
	}

	@Test
	public void testDeepStarNegative() {
		assertNotMatches("/abc/**", "/def/123");
		assertNotMatches("/**/123/456.txt", "/789/456.txt");
		assertNotMatches("/**/123/456.txt", "/abc/def/123/456.js");
		assertNotMatches("/**/*.js", "/abc/def/script.txt");
	}

}
