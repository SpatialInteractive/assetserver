package net.rcode.assetserver.core;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Core object representing the asset tree.  The asset root contains an
 * arbitrary number of AssetMount instances mapped to paths in the
 * AssetRoot.  When interpreting a path, the longest matching mapped path
 * is used to identify the owning AssetMount.
 * 
 * @author stella
 *
 */
public class AssetRoot {
	/**
	 * The owning server
	 */
	private AssetServer server;
	
	/**
	 * Map of path to the owning mount point.
	 */
	private Map<String, AssetMount> mountPoints=new HashMap<String, AssetMount>();
	
	/**
	 * If !null, then this pattern is the regular expression matching valid mounted
	 * paths.  The first capture group will contain the key into mountPoints that
	 * matched and the second capture group will contain the rest of the path
	 * (no slash).
	 */
	private volatile Pattern mountPattern;
	
	public AssetRoot() {
		this(null);
	}
	
	public AssetRoot(AssetServer server) {
		setServer(server);
	}
	
	public void setServer(AssetServer server) {
		this.server = server;
	}
	
	public AssetServer getServer() {
		return server;
	}
	
	/**
	 * Get the read-only map of mount points (mapping of path prefix to mount instance)
	 * @return mount point map
	 */
	public Map<String, AssetMount> getMountPoints() {
		return Collections.unmodifiableMap(mountPoints);
	}
	
	/**
	 * Add a mount point.  This cannot be called concurrently with read operations
	 * and should only be used at setup time.
	 * @param mountPoint
	 * @param mount
	 */
	public void add(String mountPoint, AssetMount mount) {
		mountPoints.put(normalizeMountPoint(mountPoint), mount);
		mountPattern=null;
	}
	
	protected String normalizeMountPoint(String mountPoint) {
		if (mountPoint.endsWith("/")) mountPoint=mountPoint.substring(0, mountPoint.length()-1);
		if (mountPoint.equals("")) mountPoint=null;
		return mountPoint;
	}
	
	protected Pattern getMountPattern() {
		Pattern ret=mountPattern;
		if (ret==null) {
			StringBuilder patternStr=new StringBuilder();
			boolean hasRoot=false;
			boolean hasNonRoot=false;
			patternStr.append("^(");
			for (String mountPoint: mountPoints.keySet()) {
				if (mountPoint==null) {
					hasRoot=true;
				} else {
					hasNonRoot=true;
					if (patternStr.length()>2) patternStr.append('|');
					patternStr.append(Pattern.quote(mountPoint));
				}
			}
			if (hasNonRoot) {
				patternStr.append(")");
				if (hasRoot) patternStr.append("?");
			} else {
				patternStr.setLength(0);
				if (hasRoot) {
					patternStr.append("^");
				} else {
					// No mount points
					return null;
				}
			}
			
			
			patternStr.append("(\\/.*)$");
			ret=Pattern.compile(patternStr.toString());
			mountPattern=ret;
		}
		return ret;
	}
	
	/**
	 * Fully resolve a full path to an AssetLocator
	 * @param fullPath
	 * @return AssetLocator or null
	 * @throws IOException 
	 */
	public AssetLocator resolve(String fullPath) throws Exception {
		AssetPath assetPath=match(fullPath);
		if (assetPath==null) return null;
		
		return assetPath.getMount().resolve(assetPath);
	}
	
	/**
	 * Match the given fullPath to an AssetPath representing the AssetMount and
	 * mount point that the resource belongs to.  The returned AssetPath may be
	 * further evaluated by applying it to the found mount.
	 * @param fullPath
	 * @return matching AssetPath or null if no mounts are matched
	 */
	public AssetPath match(String fullPath) {
		Pattern p=getMountPattern();
		if (p==null) return null;
		
		Matcher m=p.matcher(fullPath);
		if (m.matches()) {
			String mountPoint;
			String mountPath;
			if (m.groupCount()==2) {
				mountPoint=m.group(1);
				mountPath=m.group(2);
			} else {
				mountPoint=null;
				mountPath=m.group(1);
			}
			AssetMount mount=mountPoints.get(mountPoint);
			if (mount!=null) {
				try {
					return new AssetPath(mount, mountPoint, mountPath);
				} catch (IllegalArgumentException e) {
					server.getLogger().warn("Illegal path '" + fullPath + "': " + e.getMessage());
					return null;
				}
			}
		}
		return null;
	}
}
