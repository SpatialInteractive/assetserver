package net.rcode.assetserver.ejs;

import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;
import static org.junit.Assert.*;

import net.rcode.assetserver.ejs.EjsParser.LocationInfo;
import net.rcode.assetserver.util.IOUtil;

/**
 * Uses Docstring-like tests to verify that the parse stream is structured
 * correctly for various inputs.
 * 
 * @author stella
 *
 */
public class EjsParserTest {
	@Test
	public void testNoCommands() {
		verifyParseStream("NoCommands1");
	}
	
	@Test
	public void testEjsDisable() {
		verifyParseStream("EjsDisable1");
		verifyParseStream("EjsDisable2");
	}
	
	@Test
	public void testEjsEscapes() {
		verifyParseStream("EjsEscape1");
	}
	
	@Test
	public void testMain() {
		verifyParseStream("Main1");
	}
	
	private void verifyParseStream(String resourceName) {
		CharSequence contents=IOUtil.slurpResource(getClass(), resourceName + ".ejs");
		CharSequence expectedStream=null;
		try {
			expectedStream=IOUtil.slurpResource(getClass(), resourceName + ".out");
		} catch (RuntimeException e) {
			// Ignore
		}
		StringEvents events=new StringEvents();
		EjsParser parser=new EjsParser(events);
		parser.parse(contents);
		
		if (expectedStream==null) {
			System.out.println("No expected output for " + resourceName + ".  Expected output follows:");
			System.out.println(events.message);
			fail("No expected output for " + resourceName);
		}
		
		String expectedStreamStr=expectedStream.toString(),
			messagesStr=events.message.toString();
		if (!expectedStreamStr.equals(messagesStr)) {
			System.out.println("Output did not match expectations for " + resourceName + ". Actual follows:");
			System.out.println("----");
			System.out.print(messagesStr);
			System.out.println("---");
		}
		assertEquals(resourceName, expectedStreamStr, messagesStr);
	}


	private static class StringEvents implements EjsParser.Events {
		public StringBuilder message=new StringBuilder();
		
		@Override
		public void handleLiteral(CharSequence text, LocationInfo location) {
			message.append("{Literal(").append(location.getLineStart())
				.append(",").append(location.getSourceStart()).append(",")
				.append(location.getSourceEnd())
				.append(")='")
				.append(StringEscapeUtils.escapeJava(text.toString()))
				.append("'}\n");
		}

		@Override
		public void handleBlock(CharSequence script, LocationInfo location) {
			message.append("{Block(").append(location.getLineStart())
				.append(",").append(location.getSourceStart()).append(",")
				.append(location.getSourceEnd())
			.append(")='")
			.append(StringEscapeUtils.escapeJava(script.toString()))
			.append("'}\n");
		}

		@Override
		public void handleInterpolation(CharSequence script,
				LocationInfo location) {
			message.append("{Interpolation(").append(location.getLineStart())
				.append(",").append(location.getSourceStart()).append(",")
				.append(location.getSourceEnd())
				.append(")='")
			.append(StringEscapeUtils.escapeJava(script.toString()))
			.append("'}\n");
		}
		
	}
}
