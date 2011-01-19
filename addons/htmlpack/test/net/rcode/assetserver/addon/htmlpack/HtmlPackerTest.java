package net.rcode.assetserver.addon.htmlpack;

import java.io.StringReader;

import org.junit.Test;
import static org.junit.Assert.*;

import org.xml.sax.InputSource;

public class HtmlPackerTest {

	@Test
	public void testTagNames() throws Exception {
		verifyPack("['p']", "<p>");
		verifyPack("['p#myid']", "<p id='myid'>");
		verifyPack("['p#myid.class1']", "<p id='myid' class='class1'>");
		verifyPack("['p#myid.class1']", "<p id='myid' class=' class1'>");
		verifyPack("['p#myid.class1']", "<p id='myid' class='class1 '>");
		verifyPack("['p#myid.class1.class2']", "<p id='myid' class='class1 class2'>");
	}
	
	@Test
	public void testWithAttributes() throws Exception {
		verifyPack("['div','@someattr','somevalue']", "<div someattr='somevalue'>");
		verifyPack("['input','@type','submit']", "<input type='submit'>");
		verifyPack("['tr',['td','col1']]", "<tr><td>col1</td></tr>");
	}
	
	private void verifyPack(String expected, String html) throws Exception {
		HtmlPacker packer=HtmlPacker.parse(new InputSource(new StringReader(html)));
		packer=packer.selectFirstChild();
		String actual=packer.pack().toString();
		assertEquals(expected, actual);
	}
}
