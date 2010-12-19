package net.rcode.assetserver.ejs;

import org.junit.Test;
import static org.junit.Assert.*;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class EjsRuntimeTest {
	@Test
	public void testDoesntWrite() {
		EjsRuntime runtime=new EjsRuntime(true);	// Seal it so we get exceptions on write
		
		Context cx=Context.enter();
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

			// Enumerate Object.prototype methods
			/*
			cx.evaluateString(scope, 
					"props=[]; for (var k in Math) props.push(k); props.sort(); propString=props.join(',');", 
					"", 1, null);
			check=cx.evaluateString(scope, 
					"propString", 
					"", 1, null);
			System.out.println("Props: " + check);
			*/
		} finally {
			Context.exit();
		}
	}
}
