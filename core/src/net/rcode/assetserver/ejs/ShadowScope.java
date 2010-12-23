package net.rcode.assetserver.ejs;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Wraps a non-sealed scope such that light-weight "expando" objects are
 * returned instead of the base objects.  This works around Rhino's odd idea
 * that sealing the standard library is a good thing, allowing a non-sealed
 * shared scope to be accessed by multiple threads such that the shared
 * objects are never modified.
 * <p>
 * The facade is maintained by way of handing out new objects with their prototypes
 * set.  In normal JavaScript, these prototypes will not be writable, but Java
 * code can circumvent the protections easily (but shouldn't).
 * <p>
 * This instance will keep hard references to all replaced objects, so it important
 * that it not be used in such a way that excessive transient objects are generated.
 * 
 * @author stella
 *
 */
public class ShadowScope {
	private static final Object DELETED=new Object();
	
	private Scriptable originalScope;
	private Scriptable shadowScope;
	private IdentityHashMap<Object, Scriptable> replacementMap=new IdentityHashMap<Object, Scriptable>();
	
	private class ShadowScriptable extends ScriptableObject {
		protected Scriptable delegate;
		
		public ShadowScriptable(Scriptable delegate) {
			this.delegate=delegate;
		}
		
		@Override
		public Object get(int index, Scriptable start) {
			Object ret=super.get(index, start);
			if (ret==Scriptable.NOT_FOUND) {
				return replace(delegate.get(index, delegate));
			} else if (ret==DELETED) return Scriptable.NOT_FOUND;
			return ret;
		}
		
		@Override
		public Object get(String name, Scriptable start) {
			Object ret=super.get(name, start);
			if (ret==Scriptable.NOT_FOUND) {
				return replace(delegate.get(name, delegate));
			} else if (ret==DELETED) return Scriptable.NOT_FOUND;
			return ret;
		}

		@Override
		 public Object[] getIds() {
			Object[] ret;
			Object[] superIds=super.getIds();
			if (superIds.length==0) {
				// Common case - no local slots set
				ret=delegate.getIds();
				return ret;
			}
			
			// Uncommon case - must merge ids
			Set<Object> ids=new HashSet<Object>(Arrays.asList(superIds));
			ids.addAll(Arrays.asList(delegate.getIds()));
			
			ret=ids.toArray();
			return ret;
		}
		
		@Override
		public void delete(String name) {
			put(name, this, DELETED);
		}
		
		@Override
		public void delete(int index) {
			put(index, this, DELETED);
		}
		
		@Override
		public String getClassName() {
			return delegate.getClassName();
		}

		@Override
		public Object getDefaultValue(Class<?> hint) {
			return replace(delegate.getDefaultValue(hint));
		}
	}
	
	private class ShadowCallable extends ShadowScriptable implements Function {

		public ShadowCallable(Scriptable delegate) {
			super(delegate);
		}

		@Override
		public Object call(Context cx, Scriptable scope, Scriptable thisObj,
				Object[] args) {
			// Unwrap the this pointer before invoking.  Native
			// methods get tricked out if there "this" is not precisely
			// what was expected.
			if (delegate instanceof NativeJavaMethod) {
				if (thisObj instanceof ShadowScriptable) {
					thisObj=((ShadowScriptable)thisObj).delegate;
				}
			}
			
			return ((Function)delegate).call(cx, scope, thisObj, args);
		}

		@Override
		public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
			return ((Function)delegate).construct(cx, scope, args);
		}
		
	}
	
	public ShadowScope(Scriptable originalScope) {
		this.originalScope=originalScope;
		this.shadowScope=(Scriptable) replace(originalScope);
		
		// Various bits of Rhino depend on being able to traverse
		// up the scope chain and then up the prototype chain to find
		// the top-level scope.  The following doesn't hurt anything
		// but ensures the appropriate inheritance hierarchy.
		this.shadowScope.setPrototype(originalScope);
	}
	
	public Scriptable getScope() {
		return shadowScope;
	}
	
	private Object replace(Object in) {
		if (in==null) return null;
		
		if (in instanceof Scriptable) {
			Scriptable ret=replacementMap.get(in);
			if (ret==null) {
				if (in instanceof Function) 
					ret=new ShadowCallable((Scriptable) in);
				else
					ret=new ShadowScriptable((Scriptable) in);
				
				if (ret!=null)
					replacementMap.put(in, ret);
			}
			return ret;
		} else {
			return in;
		}
	}
}
