package net.rcode.assetserver.core;

/**
 * Overall server configuration options.  This class breaks out config settings
 * as discreet properties.  At a future point, it may have a more flexible
 * access mechanism.
 * <h2>Property Overview</h2>
 * <ul>
 * <li>httpNoCache (default=true): If true, then HTTP handlers will output cache headers that
 *     disable the browser from caching the content.
 * <li>noOptimize (default=false): If true, then any resource filters that involve optimizing
 *     or obfuscating a resource will be disabled
 * </ul>
 * 
 * @author stella
 *
 */
public class ServerConfig {
	private boolean httpNoCache=true;
	private boolean noOptimize;
	
	public boolean isHttpNoCache() {
		return httpNoCache;
	}
	public void setHttpNoCache(boolean httpNoCache) {
		this.httpNoCache = httpNoCache;
	}
	
	public boolean isNoOptimize() {
		return noOptimize;
	}
	public void setNoOptimize(boolean noOptimize) {
		this.noOptimize = noOptimize;
	}
}
