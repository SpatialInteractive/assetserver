package net.rcode.assetserver.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class MimeMappingTest {

	@Test
	public void testAll() {
		MimeMapping mapping=new MimeMapping();
		mapping.loadDefaults();
		
		assertEquals("text/javascript", mapping.lookup("test.js"));
		assertNull(mapping.lookup("unknownfile.xxx"));
	}
}
