package net.rcode.assetserver.util;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class GenericScriptExceptionTest {

	@Test
	public void testErrorFromScript() {
		try {
			evaluateScript("myscript.js", "var a=1;\nthrow new Error('This is a script runtime Error');");
		} catch (Exception e) {
			Exception ge=GenericScriptException.translateException(e);
			ge.printStackTrace();
		}
	}

	private void evaluateScript(String source, String script) {
		Context cx=Context.enter();
		try {
			Scriptable scope=cx.initStandardObjects();
			cx.evaluateString(scope, script, source, 1, null);
		} finally {
			Context.exit();
		}
	}
}
