package net.rcode.assetserver.util;

import net.rcode.assetserver.util.NamePattern;

import org.junit.Test;
import static org.junit.Assert.*;

public class NamePatternTest {
	private NamePattern pattern;

	private void assertMatch(String name) {
		assertTrue(name + " should match", pattern.matches(name));
	}
	
	private void assertNoMatch(String name) {
		assertFalse(name + " should not match", pattern.matches(name));
	}
	
	@Test
	public void testDefaultExcludesNegative() {
		pattern = NamePattern.DEFAULT_EXCLUDES;
		
		assertNoMatch("randomfile.txt");
		assertNoMatch(".DS_Storesomethingelse");
		assertNoMatch("#notabackup");
	}
	
	@Test
	public void testDefaultExcludesPositive() {
		pattern = NamePattern.DEFAULT_EXCLUDES;

		assertMatch("somefile.txt~");
		assertMatch("#abackupfile#");
		assertMatch(".#somebackup");
		assertMatch("%somebackup%");
		assertMatch("._somethingelse");
		assertMatch("CVS");
		assertMatch(".cvsignore");
		assertMatch("SCCS");
		assertMatch("vssver.scc");
		assertMatch(".svn");
		assertMatch(".DS_Store");
		assertMatch(".git");
		assertMatch(".gitattributes");
		assertMatch(".gitignore");
		assertMatch(".gitmodules");
		assertMatch(".hg");
		assertMatch(".hgignore");
		assertMatch(".hgsub");
		assertMatch(".hgsubstate");
		assertMatch(".hgtags");
		assertMatch(".bzr");
		assertMatch(".bzrignore");
	}
}
