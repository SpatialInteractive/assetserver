package net.rcode.assetserver.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class AssetRootTest {
	private static class DummyMount extends AssetMount {

		@Override
		public AssetLocator resolve(AssetPath path) {
			return null;
		}
	}
	
	@Test
	public void testMatchEmpty() {
		AssetRoot root=new AssetRoot();
		assertNull(root.match("/some/path"));
		assertNull(root.match(""));
	}
	
	@Test
	public void testSingleNonRoot() {
		AssetRoot root=new AssetRoot();
		AssetPath match;
		DummyMount m1=new DummyMount();
		root.add("/cdn", m1);
		
		match=root.match("/not/under/cdn");
		assertNull(match);
		
		match=root.match("/cdn/file.txt");
		assertNotNull(match);
		assertEquals(match.getMount(), m1);
		assertEquals(match.getMountPoint(), "/cdn");
		assertEquals(match.getPath(), "/file.txt");
	}
	
	@Test
	public void testMultiNonRoot() {
		AssetRoot root=new AssetRoot();
		AssetPath match;
		DummyMount m1=new DummyMount(), m2=new DummyMount();
		root.add("/cdn", m1);
		root.add("/tools", m2);
		
		match=root.match("/not/under/cdn");
		assertNull(match);

		match=root.match("/cdn");
		assertNull(match);

		match=root.match("/cdn/");
		assertNotNull(match);
		assertEquals(match.getMount(), m1);
		assertEquals(match.getMountPoint(), "/cdn");
		assertEquals(match.getPath(), "/");
		
		match=root.match("/cdn/file.txt");
		assertNotNull(match);
		assertEquals(match.getMount(), m1);
		assertEquals(match.getMountPoint(), "/cdn");
		assertEquals(match.getPath(), "/file.txt");

		match=root.match("/tools/afile.js");
		assertNotNull(match);
		assertEquals(match.getMount(), m2);
		assertEquals(match.getMountPoint(), "/tools");
		assertEquals(match.getPath(), "/afile.js");
	}

	@Test
	public void testMultiWithRoot() {
		AssetRoot root=new AssetRoot();
		AssetPath match;
		DummyMount m1=new DummyMount(), m2=new DummyMount(), m3=new DummyMount();
		root.add("/cdn", m1);
		root.add("/tools", m2);
		root.add("/", m3);
		
		match=root.match("/not/under/cdn");
		assertNotNull(match);
		assertEquals(m3, match.getMount());
		assertEquals(null, match.getMountPoint());
		assertEquals("/not/under/cdn", match.getPath());

		match=root.match("/cdn/");
		assertNotNull(match);
		assertEquals(match.getMount(), m1);
		assertEquals(match.getMountPoint(), "/cdn");
		assertEquals(match.getPath(), "/");
		
		match=root.match("/cdn/file.txt");
		assertNotNull(match);
		assertEquals(match.getMount(), m1);
		assertEquals(match.getMountPoint(), "/cdn");
		assertEquals(match.getPath(), "/file.txt");

		match=root.match("/tools/afile.js");
		assertNotNull(match);
		assertEquals(match.getMount(), m2);
		assertEquals(match.getMountPoint(), "/tools");
		assertEquals(match.getPath(), "/afile.js");
	}

	@Test
	public void testJustRoot() {
		AssetRoot root=new AssetRoot();
		AssetPath match;
		DummyMount m1=new DummyMount();
		root.add("/", m1);
		
		match=root.match("/some/file.txt");
		assertNotNull(match);
		assertEquals(m1, match.getMount());
		assertEquals(null, match.getMountPoint());
		assertEquals("/some/file.txt", match.getPath());
	}
}
