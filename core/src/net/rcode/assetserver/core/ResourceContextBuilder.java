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
	
	/**
	 * Scope used to evaluate ResourceContext configuration.
	 * This scope is initialized by evaluating resourcecontext-runtime.js
	 */
	private Scriptable contextScope;
	
	/**
	 * Scope used to evaluate server configuraiton.  Inherits from contextScope.
	 * This scope is initialized by evaluating serverconfig-runtime.js
	 */
	private Scriptable serverScope;
	
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
			contextScope=cx.newObject(nativeScope);
			contextScope.setParentScope(null);
			contextScope.setPrototype(nativeScope);
			
			serverScope=cx.newObject(nativeScope);
			serverScope.setParentScope(null);
			serverScope.setPrototype(contextScope);
			
			// Set common globals
			ScriptableObject.putProperty(contextScope, "logger", logger);
			ScriptableObject.putProperty(contextScope, "context", null);

			loadLibResource(cx, contextScope, "resourcecontext-runtime.js");
			loadLibResource(cx, serverScope, "serverconfig-runtime.js");
			
			// Freeze
			((ScriptableObject)contextScope).sealObject();
			((ScriptableObject)serverScope).sealObject();
		} finally {
			exit();
		}
	}

	/**
	 * Load a library into the context
	 * @param string
	 */
	private void loadLibResource(Context cx, Scriptable scope, String resource) {
		try {
			InputStream in=getClass().getResourceAsStream(resource);
			if (in==null) throw new RuntimeException("Resource not found: " + resource);
			Reader reader=new BufferedReader(new InputStreamReader(in, "UTF-8"));
			try {
				cx.evaluateReader(scope, reader, resource, 1, null);
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
	public void evaluateAsAccess(ResourceContext target, String script, String sourceName) {
		try {
			evaluateAsAccess(target, new StringReader(script), sourceName);
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
	public void evaluateAsAccess(ResourceContext target, Reader script, String sourceName) throws IOException {
		Context cx=enter();
		try {
			Scriptable evalScope=cx.newObject(contextScope);
			evalScope.setParentScope(null);
			evalScope.setPrototype(contextScope);
			
			ScriptableObject.putProperty(evalScope, "context", target);
			cx.evaluateReader(evalScope, script, sourceName, 1, null);
		} finally {
			exit();
		}
	}
	
	/**
	 * Configure the server and rootContext with the given script
	 * @param rootContext
	 * @param server
	 * @param script
	 * @param sourceName
	 * @throws IOException
	 */
	public void evaluateServerConfig(ResourceContext rootContext, AssetServer server, Reader script, String sourceName) throws IOException {
		Context cx=enter();
		try {
			Scriptable evalScope=cx.newObject(serverScope);
			evalScope.setParentScope(null);
			evalScope.setPrototype(serverScope);
			
			ScriptableObject.putProperty(evalScope, "context", rootContext);
			ScriptableObject.putProperty(evalScope, "server", server);
			
			cx.evaluateReader(evalScope, script, sourceName, 1, null);
		} finally {
			exit();
		}
	}
}
