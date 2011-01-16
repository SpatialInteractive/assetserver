package net.rcode.assetserver.util;

import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

/**
 * Some rhino utilities
 * @author stella
 *
 */
public class RhinoUtil {
	/**
	 * A ContextFactory that can control some feature enablement
	 * via configuration.
	 * @author stella
	 *
	 */
	public static class ConfigurableContextFactory extends ContextFactory {
		private boolean useDynamicScope;
		
		public boolean isUseDynamicScope() {
			return useDynamicScope;
		}
		public void setUseDynamicScope(boolean useDynamicScope) {
			this.useDynamicScope = useDynamicScope;
		}
		
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
	
	/**
	 * Wrap a Map as a Scriptable
	 * @author stella
	 */
	public static class MapScriptable implements Scriptable {
		private Scriptable scope;
		private Map delegate;
		
		public MapScriptable(Map delegate, Scriptable scope) {
			this.delegate=delegate;
		}
		
		@Override
		public String getClassName() {
			return null;
		}

		@Override
		public Object get(String name, Scriptable start) {
			Object value=delegate.get(name);
			if (value==null) return NOT_FOUND;
			return Context.javaToJS(value, scope);
		}

		@Override
		public Object get(int index, Scriptable start) {
			return get(String.valueOf(index), start);
		}

		@Override
		public boolean has(String name, Scriptable start) {
			return delegate.containsKey(name);
		}

		@Override
		public boolean has(int index, Scriptable start) {
			return false;
		}

		@Override
		public void put(String name, Scriptable start, Object value) {
			delegate.put(name, value);
		}

		@Override
		public void put(int index, Scriptable start, Object value) {
			put(String.valueOf(index), start, value);
		}

		@Override
		public void delete(String name) {
			delegate.remove(name);
		}

		@Override
		public void delete(int index) {
			delegate.remove(String.valueOf(index));
		}

		@Override
		public Scriptable getPrototype() {
			return null;
		}

		@Override
		public void setPrototype(Scriptable prototype) {
			throw new IllegalStateException("setPrototype not implemented");
		}

		@Override
		public Scriptable getParentScope() {
			return scope;
		}

		@Override
		public void setParentScope(Scriptable parent) {
			scope=parent;
		}

		@Override
		public Object[] getIds() {
			return delegate.keySet().toArray();
		}

		@Override
		public Object getDefaultValue(Class<?> hint) {
			return null;
		}

		@Override
		public boolean hasInstance(Scriptable instance) {
			return false;
		}
		
	}
	
}
