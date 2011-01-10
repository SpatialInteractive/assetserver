package net.rcode.assetserver.util;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class PathUtilTest {
	
	@Test
	public void testJoinEmpty() {
		assertEquals("/", PathUtil.joinPath());
	}
	
	@Test
	public void testJoin() {
		assertEquals("/abc/123.txt", PathUtil.joinPath("abc", "123.txt"));
	}
	
	@Test
	public void testSplitRoot() {
		assertEquals(0, PathUtil.splitPath("/").length);
	}
	
	@Test
	public void testSplit() {
		assertTrue(Arrays.equals(new String[] { "abc", "123" }, PathUtil.splitPath("/abc/123")));
		assertTrue(Arrays.equals(new String[] { "abc", "123" }, PathUtil.splitPath("/abc/123/")));
		assertTrue(Arrays.equals(new String[] { "abc", "123" }, PathUtil.splitPath("abc/123/")));
	}
	
	@Test
	public void testNormalizeAbsolute() {
		assertEquals("/abc/def/123", PathUtil.normalizePath("/", "/abc/def/123"));
		assertEquals("/abc/def/123", PathUtil.normalizePath("/", "/abc/def/./123"));
		assertEquals("/abc/def/123", PathUtil.normalizePath("/", "/abc/nothere/../def/123"));
		
		assertNull(PathUtil.normalizePath("/", "/abc/def/123/../../../.."));
	}

	@Test
	public void testNormalizeRelative() {
		assertEquals("/root/abc/def/123", PathUtil.normalizePath("/root", "abc/def/123"));
		assertEquals("/root/abc/def/123", PathUtil.normalizePath("/root", "abc/def/./123"));
		assertEquals("/root/abc/def/123", PathUtil.normalizePath("/root", "abc/nothere/../def/123"));
		
		assertEquals("/", PathUtil.normalizePath("/root", "abc/def/123/../../../../"));
		assertNull(PathUtil.normalizePath("/root", "abc/def/123/../../../../.."));
	}

	@Test
	public void testNormalizeDoubleSlash() {
		assertEquals("/abc/def/123", PathUtil.normalizePath("/", "/abc//def//123///"));
	}
	
	@Test
	public void testTranslateRelative() {
		assertEquals("menustyles/images/bluedot.png", PathUtil.translateRelative("/content/css/menustyles", "/content/css", "images/bluedot.png"));
		assertEquals("../../content/css/menustyles/images/bluedot.png", PathUtil.translateRelative("/content/css/menustyles/", "/jquery/css", "images/bluedot.png"));
	}

}
