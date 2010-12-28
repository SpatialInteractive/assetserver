package net.rcode.assetserver.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;
import org.mozilla.javascript.EvaluatorException;

public class YuiCompressorTest {

	@Test
	public void testCompressJs() {
		YuiCompressor compressor=new YuiCompressor();
		String src="(function(somevarname) { print(somevarname); })();";
		StringWriter out=new StringWriter();
		compressor.compressJs(new StringReader(src), out);
		out.flush();
		//System.out.println(out.toString());
		assertEquals("(function(a){print(a)})();", out.toString());
	}
	
	@Test
	public void testCompressCss() {
		YuiCompressor compressor=new YuiCompressor();
		String src="#someid  { background-color : blue; }";
		StringWriter out=new StringWriter();
		compressor.compressCss(new StringReader(src), out);
		out.flush();
		//System.out.println(out.toString());
		assertEquals("#someid{background-color:blue;}", out.toString());
	}
	
	@Test
	public void testJsError() {
		YuiCompressor compressor=new YuiCompressor();
		String src="abc'123";
		StringWriter out=new StringWriter();
		try {
			compressor.compressJs(new StringReader(src), out);
			fail("Expected exception");
		} catch (EvaluatorException e) {
			assertEquals("unterminated string literal", e.getMessage());
		}
	}
}
