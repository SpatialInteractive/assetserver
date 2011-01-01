package net.rcode.assetserver.core;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

/**
 * Test AssetPath
 * @author stella
 *
 */
public class AssetPathTest {
	private AssetMount dummyMount=new AssetMount() {
		@Override
		public AssetLocator resolve(AssetPath path) {
			// TODO Auto-generated method stub
			return null;
		}
	};
	
	@Test
	public void testTrivialBaseFields() {
		AssetPath p;
		
		// Test a non-trival mount and path
		p=new AssetPath(dummyMount, null, "/");
		assertTrue(Arrays.equals(new Object[] { }, p.getMountPointComponents()));
		assertTrue(Arrays.equals(new Object[] { }, p.getPathComponents()));
		assertEquals(null, p.getBaseName());
		assertEquals(null, p.getMountPoint());
		assertEquals("/", p.getPath());
		assertEquals("", p.getFullPath());
		assertNull(p.getParameterString());
		assertEquals(dummyMount, p.getMount());
	}
	
	@Test
	public void testNonTrivialBaseFields() {
		AssetPath p;
		
		// Test a non-trival mount and path
		p=new AssetPath(dummyMount, "/cdn", "/some/file.txt");
		assertTrue(Arrays.equals(new Object[] { "cdn" }, p.getMountPointComponents()));
		assertTrue(Arrays.equals(new Object[] { "some", "file.txt" }, p.getPathComponents()));
		assertEquals("file.txt", p.getBaseName());
		assertEquals("/cdn", p.getMountPoint());
		assertEquals("/some/file.txt", p.getPath());
		assertEquals("/cdn/some/file.txt", p.getFullPath());
		assertNull(p.getParameterString());
		assertEquals(dummyMount, p.getMount());
	}

	@Test
	public void testParameterString() {
		AssetPath p;
		
		// Test a non-trival mount and path
		p=new AssetPath(dummyMount, "/cdn", "/some/file$name=value&other=some$.txt");
		assertTrue(Arrays.equals(new Object[] { "cdn" }, p.getMountPointComponents()));
		assertTrue(Arrays.equals(new Object[] { "some", "file.txt" }, p.getPathComponents()));
		assertEquals("file.txt", p.getBaseName());
		assertEquals("/cdn", p.getMountPoint());
		assertEquals("/cdn/some/file.txt", p.getFullPath());
		assertEquals("/some/file$name=value&other=some$.txt", p.getPath());
		assertEquals("name=value&other=some", p.getParameterString());
		assertEquals(dummyMount, p.getMount());
	}
	
	@Test
	public void testIllegalValues() {
		// Illegal characters in path
		assertInvalid("/root", "/%2f.txt");	// Encoded forward slash
		assertInvalid("/root", "/\\.txt");
		assertInvalid("/root", "/:txt");
		assertInvalid("/root", "/\"txt");
		assertInvalid("/root", "/'.txt");
		assertInvalid("/root", "/<txt");
		assertInvalid("/root", "/>txt");
		assertInvalid("/root", "/|txt");
		assertInvalid("/root", "/?txt");
		assertInvalid("/root", "/*txt");
		
		// Illegal names in path
		assertInvalid("/root", "/./somefile.txt");
		assertInvalid("/root", "/../somefile.txt");
		assertInvalid("/root", "/con/somefile.txt");
		assertInvalid("/root", "/PRN/somefile.txt");
		assertInvalid("/root", "/AUX/somefile.txt");
		assertInvalid("/root", "/NUL/somefile.txt");
		assertInvalid("/root", "/COM3/somefile.txt");
		assertInvalid("/root", "/LPT5/somefile.txt");
	}
	
	private void assertInvalid(String mountPoint, String path) {
		try {
			AssetPath ap=new AssetPath(dummyMount, mountPoint, path);
			fail("Expected invalid for '" + mountPoint + "', '" + path + "'");
		} catch (IllegalArgumentException e) {
			// Expect exception
		}
	}
}
