package net.rcode.assetserver.ejs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;

import net.rcode.assetserver.util.GenericScriptException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the core EJS runtime.  An instance of this class should be held
 * globally and used to evaluate or build EJS scripts.
 * 
 * @author stella
 *
 */
public class EjsRuntime {
	private static Logger ejsLogger=LoggerFactory.getLogger("ejs");
	
	private ScriptableObject sharedScope;
	private Scriptable hostObjects;
	private boolean useDynamicScope;
	private LocalFactory contextFactory=new LocalFactory();
	
	// HACK ALERT: We need the ability to do nested top calls.
	// Rhino doesn't provide this, so we have to access some package private
	// fields to make it work
	/**
	 * Fields on Context that should be preserved/reset to null to do a nested
	 * top call
	 */
	private Field[] topCallContextPreserve;
	
	private Field accessPrivateField(Class t, String name) {
		for (Field f: t.getDeclaredFields()) {
			if (name.equals(f.getName())) {
				f.setAccessible(true);
				return f;
			}
		}
		
		throw new IllegalStateException("Field not found " + name);
	}
	
	private void initReflectionBits() {
		// Look at ScriptRuntime.doTopCall to figure out what gets set an reset
		topCallContextPreserve=new Field[] {
			accessPrivateField(Context.class, "topCallScope"),
			accessPrivateField(Context.class, "cachedXMLLib"),
			accessPrivateField(Context.class, "currentActivationCall")
		};
	}
	
	public <T> T doNestedTopCall(Context cx, Callable<T> callable) throws Exception {
		// Preserve fields
		Object[] values=new Object[topCallContextPreserve.length];
		for (int i=0; i<values.length; i++) {
			Field f=topCallContextPreserve[i];
			values[i]=f.get(cx);
			f.set(cx, null);
		}
		try {
			return callable.call();
		} finally {
			// Put 'em back
			for (int i=0; i<values.length; i++) {
				topCallContextPreserve[i].set(cx, values[i]);
			}
		}
	}
	
	private class LocalFactory extends ContextFactory {
		@Override
		protected boolean hasFeature(Context cx, int featureIndex) {
			if (featureIndex == Context.FEATURE_DYNAMIC_SCOPE) {
				return useDynamicScope;
			} else if (featureIndex == Context.FEATURE_LOCATION_INFORMATION_IN_ERROR) {
				return true;
			}
			return super.hasFeature(cx, featureIndex);
		}
		
	}
	
	public class Instance {
		private Scriptable scope;
		
		private Instance() {
			scope=createRuntimeScope();
		}
		
		public Scriptable getScope() {
			return scope;
		}
		
		public Object evaluate(String source) {
			Context cx=enter();
			try {
				return cx.evaluateString(scope, source, null, 1, null);
			} finally {
				exit();
			}
		}
		
		/**
		 * Create a JavaScript function that coerces its first argument
		 * to a String and writes it to the appendable passed to this function
		 * @param a
		 * @return Function(value)
		 */
		public Function createAppendableAdapter(Appendable a) {
			String src="function(appendable) { return function(value) { appendable.append(String(value)); } }";
			Context cx=enter();
			try {
				return (Function) cx.compileFunction(scope, src, "", 1, null)
					.call(cx, scope, null, new Object[] { a });
			} finally {
				exit();
			}
		}
	}
	
	public Instance createInstance() {
		return new Instance();
	}
	
	public Context enter() {
		return contextFactory.enterContext();
	}
	
	public void exit() {
		Context.exit();
	}
	
	EjsRuntime(boolean seal) {
		initReflectionBits();
		
		useDynamicScope=true;
		Context cx=enter();
		try {
			sharedScope=cx.initStandardObjects(null, seal);
			ScriptableObject.putProperty(sharedScope, "global", sharedScope);
			ScriptableObject.putProperty(sharedScope, "logger", Context.javaToJS(ejsLogger, sharedScope));
			
			hostObjects=cx.newObject(sharedScope);
			ScriptableObject.putProperty(sharedScope, "hostobjects", hostObjects);
			
			if (seal) sharedScope.sealObject();
		} finally {
			exit();
		}
	}
	
	public EjsRuntime() {
		this(false);
	}
	
	public ScriptableObject getSharedScope() {
		return sharedScope;
	}
	
	public Scriptable getHostObjects() {
		return hostObjects;
	}
	
	public Scriptable createRuntimeScope() {
		Context ctx=Context.enter();
		
		try {
			ShadowScope shadowScope=new ShadowScope(sharedScope);
			return shadowScope.getScope();
		} finally {
			Context.exit();
		}
	}
	
	public Object javaToJs(Object java) {
		return Context.javaToJS(java, sharedScope);
	}
	
	public void loadLibrary(Reader source, String name) throws Exception {
		try {
			Context cx=enter();
			//useDynamicScope=true;
			try {
				cx.evaluateReader(sharedScope, source, name, 1, null);
			} finally {
				//useDynamicScope=false;
				exit();
				source.close();
			}
		} catch (IOException e) {
			throw new RuntimeException("IO error loading library " + name, e);
		} catch (Exception e) {
			throw GenericScriptException.translateException(e);
		}
	}
	
	public void loadLibraryStd() throws Exception {
		loadLibrary(getClass(), "stdlib.js");
	}
	
	public void loadLibrary(Class<?> relativeTo, String resourceName) throws Exception {
		try {
			InputStream in=relativeTo.getResourceAsStream(resourceName);
			if (in==null) {
				throw new RuntimeException("Resource not found " + resourceName);
			}
			Reader reader=new InputStreamReader(in, "UTF-8");
			loadLibrary(reader, resourceName);
		} catch (IOException e) {
			throw new RuntimeException("IO error loading library " + relativeTo.getName() + "/" + resourceName, e);
		}
	}
	
}
