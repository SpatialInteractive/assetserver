package net.rcode.assetserver.core;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test AssetPath
 * @author stella
 *
 */
public class AssetPathTest {
	private AssetMount dummyMount=new AssetMount() {
		@Override
		public AssetLocator resolve(String mountPath) {
			// TODO Auto-generated method stub
			return null;
		}
	};
	
	@Test
	public void testTrivialBaseFields() {
		AssetPath p;
		
		// Test a non-trival mount and path
		p=new AssetPath(dummyMount, null, "/");
		assertTrue(p.isValid());
		assertTrue(Arrays.equals(new Object[] { }, p.getMountPointComponents()));
		assertTrue(Arrays.equals(new Object[] { }, p.getPathComponents()));
		assertEquals(null, p.getBaseName());
		assertEquals(null, p.getMountPoint());
		assertEquals("/", p.getPath());
		assertNull(p.getParameterString());
		assertEquals(dummyMount, p.getMount());
	}
	
	@Test
	public void testNonTrivialBaseFields() {
		AssetPath p;
		
		// Test a non-trival mount and path
		p=new AssetPath(dummyMount, "/cdn", "/some/file.txt");
		assertTrue(p.isValid());
		assertTrue(Arrays.equals(new Object[] { "cdn" }, p.getMountPointComponents()));
		assertTrue(Arrays.equals(new Object[] { "some", "file.txt" }, p.getPathComponents()));
		assertEquals("file.txt", p.getBaseName());
		assertEquals("/cdn", p.getMountPoint());
		assertEquals("/some/file.txt", p.getPath());
		assertNull(p.getParameterString());
		assertEquals(dummyMount, p.getMount());
	}

	@Test
	public void testParameterString() {
		AssetPath p;
		
		// Test a non-trival mount and path
		p=new AssetPath(dummyMount, "/cdn", "/some/file$name=value&other=some$.txt");
		assertTrue(p.isValid());
		assertTrue(Arrays.equals(new Object[] { "cdn" }, p.getMountPointComponents()));
		assertTrue(Arrays.equals(new Object[] { "some", "file.txt" }, p.getPathComponents()));
		assertEquals("file.txt", p.getBaseName());
		assertEquals("/cdn", p.getMountPoint());
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
		AssetPath ap=new AssetPath(dummyMount, mountPoint, path);
		assertFalse("Expected invalid for '" + mountPoint + "', '" + path + "'", ap.isValid());
	}
}
