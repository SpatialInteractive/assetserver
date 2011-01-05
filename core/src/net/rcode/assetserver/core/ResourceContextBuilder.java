package net.rcode.assetserver.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import net.rcode.assetserver.util.RhinoUtil.ConfigurableContextFactory;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Construct a ResourceContext from a .asaccess JavaScript file and a parent.  Creating
 * an instance of this class is a heavy-weight operation and should not be done repetitively
 * because it initializes the JavaScript execution environment.
 * 
 * @author stella
 *
 */
public class ResourceContextBuilder {
	private final Logger logger=LoggerFactory.getLogger("contextbuilder");
	private Scriptable nativeScope;
	private Scriptable libScope;
	private ConfigurableContextFactory contextFactory=new ConfigurableContextFactory();
	
	private Context enter() {
		return contextFactory.enterContext();
	}
	
	private void exit() {
		Context.exit();
	}
	
	public ResourceContextBuilder() {
		Context cx=enter();
		try {
			// Initialize scopes
			nativeScope=cx.initStandardObjects(null, true);
			libScope=cx.newObject(nativeScope);
			libScope.setParentScope(null);
			libScope.setPrototype(nativeScope);
			
			// Set common globals
			ScriptableObject.putProperty(libScope, "logger", logger);
			
			// Load runtime library
			contextFactory.setUseDynamicScope(true);
			loadLibResource(cx, "resourcecontext-runtime.js");
		} finally {
			contextFactory.setUseDynamicScope(false);
			exit();
		}
	}

	/**
	 * Load a library into the context
	 * @param string
	 */
	private void loadLibResource(Context cx, String resource) {
		try {
			InputStream in=getClass().getResourceAsStream(resource);
			if (in==null) throw new RuntimeException("Resource not found: " + resource);
			Reader reader=new BufferedReader(new InputStreamReader(in, "UTF-8"));
			try {
				cx.evaluateReader(libScope, reader, resource, 1, null);
			} finally {
				reader.close();
			}
		} catch (IOException e) {
			throw new RuntimeException("IO error evaluating resource " + resource, e);
		}
	}
}
