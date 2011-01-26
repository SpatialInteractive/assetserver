package net.rcode.assetserver.ejs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class EjsRuntimeTest {
	@Test
	public void testDoesntWrite() {
		EjsRuntime runtime=new EjsRuntime(true);	// Seal it so we get exceptions on write
		
		Context cx=runtime.enter();
		Scriptable scope=runtime.createRuntimeScope();
		//Scriptable scope=runtime.getSharedScope();
		
		//Object value=cx.javaToJS("Some String", scope);
		Object check;
		try {
			// Set something on a sealed native object
			cx.evaluateString(scope, 
					"Object.value='Testing';", 
					"", 1, null);
			check=cx.evaluateString(scope, 
					"Object.value", 
					"", 1, null);
			assertEquals("Testing", check.toString());
			
			// Delete it and make sure it is undefined
			cx.evaluateString(scope, 
					"delete Object.value;", 
					"", 1, null);
			check=cx.evaluateString(scope, 
					"Object.value", 
					"", 1, null);
			assertEquals(Context.getUndefinedValue(), check);
			
			// Make sure we can call a function
			check=cx.evaluateString(scope, 
					"new Date().getTime()", 
					"", 1, null);
			assertTrue(((Number)cx.jsToJava(check, Number.class)).longValue()>0);
			
			// And set an indexed property for good measure
			cx.evaluateString(scope, 
					"Object[1]='Testing';", 
					"", 1, null);
			check=cx.evaluateString(scope, 
					"Object[1]", 
					"", 1, null);
			assertEquals("Testing", check.toString());

		} finally {
			runtime.exit();
		}
	}
	
	@Test
	public void testStdLib() throws Exception {
		EjsRuntime runtime=new EjsRuntime();
		runtime.loadLibraryStd();
		
		EjsRuntime.Instance instance=runtime.createInstance();
		Object check;
		
		// Check String.prototype additions
		check=instance.evaluate("'string with a \\r\\'\"'.toJs()");
		assertEquals("string with a \\r\\'\\\"", check.toString());
		
		check=instance.evaluate("'java string \"'.toJava()");
		assertEquals("java string \\\"", check.toString());
		
		check=instance.evaluate("'some <html id=\"test\" />'.toHtml()");
		assertEquals("some &lt;html id=&quot;test&quot; /&gt;", check.toString());

		check=instance.evaluate("'some <html id=\"test\" />'.toXml()");
		assertEquals("some &lt;html id=&quot;test&quot; /&gt;", check.toString());

		//System.out.println("" + check);
		
		// Check logging
		instance.evaluate("logger.debug('Debug log message')");
		instance.evaluate("logger.info('Info log message')");
		instance.evaluate("logger.warn('Debug log message')");
		instance.evaluate("logger.error('error log message', new Error('Exception'))");
	}
}
