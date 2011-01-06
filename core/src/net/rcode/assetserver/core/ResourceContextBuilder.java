package net.rcode.assetserver.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

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
		contextFactory.setUseDynamicScope(true);
		Context cx=enter();
		try {
			// Initialize scopes
			nativeScope=cx.initStandardObjects(null, true);
			libScope=cx.newObject(nativeScope);
			libScope.setParentScope(null);
			libScope.setPrototype(nativeScope);
			
			// Set common globals
			ScriptableObject.putProperty(libScope, "logger", logger);
			ScriptableObject.putProperty(libScope, "context", null);
		} finally {
			exit();
		}

		// Reload the context with a dynamic scope
		// Load runtime library
		cx=enter();
		try {
			loadLibResource(cx, "resourcecontext-runtime.js");
			
			// Freeze
			((ScriptableObject)libScope).sealObject();
		} finally {
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
	
	/**
	 * Convenience that uses a script specified as a String
	 * @param target
	 * @param script
	 * @param sourceName
	 */
	public void buildContext(ResourceContext target, String script, String sourceName) {
		try {
			buildContext(target, new StringReader(script), sourceName);
		} catch (IOException e) {
			throw new RuntimeException("Unexpected IO exception evaluating string", e);
		}
	}
	
	/**
	 * Build the given target context using the script to be read
	 * from reader, using the given sourceName for debugging.
	 * @param target
	 * @param script Reader (not closed on completion)
	 * @param sourceName
	 * @throws IOException 
	 */
	public void buildContext(ResourceContext target, Reader script, String sourceName) throws IOException {
		Context cx=enter();
		try {
			Scriptable evalScope=cx.newObject(libScope);
			evalScope.setParentScope(null);
			evalScope.setPrototype(libScope);
			
			ScriptableObject.putProperty(evalScope, "context", target);
			cx.evaluateReader(evalScope, script, sourceName, 1, null);
		} finally {
			exit();
		}
	}
}
