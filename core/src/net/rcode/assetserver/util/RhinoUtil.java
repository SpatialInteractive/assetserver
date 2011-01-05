package net.rcode.assetserver.util;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

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
}
