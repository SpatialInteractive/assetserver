package net.rcode.assetserver.ejs;

import java.io.IOException;
import java.io.Reader;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Encapsulates the core EJS runtime.  An instance of this class should be held
 * globally and used to evaluate or build EJS scripts.
 * 
 * @author stella
 *
 */
public class EjsRuntime {
	private ScriptableObject sharedScope;
	private boolean useDynamicScope;
	private LocalFactory contextFactory=new LocalFactory();
	
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
	
	public Context enter() {
		return contextFactory.enterContext();
	}
	
	public void exit() {
		Context.exit();
	}
	
	EjsRuntime(boolean seal) {
		Context ctx=Context.enter();
		try {
			sharedScope=ctx.initStandardObjects(null, seal);
			ScriptableObject.putProperty(sharedScope, "global", sharedScope);
			
			if (seal) sharedScope.sealObject();
		} finally {
			Context.exit();
		}
	}
	
	public EjsRuntime() {
		this(false);
	}
	
	public ScriptableObject getSharedScope() {
		return sharedScope;
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
	
	public void loadLibrary(Reader source) throws IOException {
		Context cx=enter();
		useDynamicScope=true;
		try {
			cx.evaluateReader(sharedScope, source, "", 1, null);
		} finally {
			useDynamicScope=false;
			exit();
		}
	}
	
	
	/*
	public static void main(String[] args) throws Exception {
		int total=10000;
		EjsRuntime er=new EjsRuntime();
		
		long start=System.currentTimeMillis();
		long runtime=System.currentTimeMillis()-start;
		System.out.println("Avg time=" + (runtime / (double)total) + "ms");
		
		Scriptable scope=er.createRuntimeScope();
		Context cx=Context.enter();
		try {
			//Object value=scope.get("Object", scope);
			Object object=ScriptableObject.getProperty(scope, "Object");
			System.out.println("Object=" + object);
			Object prototype=ScriptableObject.getProperty((Scriptable) object, "prototype");
			System.out.println("Object.prototype=" + prototype);
			Object toString=ScriptableObject.getProperty((Scriptable) prototype, "toString");
			System.out.println("toString=" + toString);
		} finally {
			Context.exit();
		}
	}*/
}
