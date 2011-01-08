package net.rcode.assetserver.core;

import java.io.IOException;
import java.util.Collection;
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
		if (mountPoint==null) return null;
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
	
	/**
	 * Checks to see whether the given AssetPath can be resolved by another
	 * mount point that is more specific than this one.
	 * 
	 * @param assetPath
	 * @return true if overlapped
	 * @throws Exception 
	 */
	private boolean overlapped(AssetPath assetPath) throws Exception {
		Pattern p=getMountPattern();
		if (p==null) return false;
		
		Matcher m=p.matcher(assetPath.getFullPath());
		if (m.matches()) {
			String mountPoint;
			if (m.groupCount()==2) {
				mountPoint=m.group(1);
			} else {
				mountPoint=null;
			}
			AssetMount mount=mountPoints.get(mountPoint);
			if (mount==null || mount==assetPath.getMount()) return false;
			else return true;
		} else {
			return false;
		}
	}

	/**
	 * Scans the namespace as defined by config, invoking the given callback.
	 * TODO: Add symlink cycle detection.  Not critical right now because the ResourceMount
	 * explicitly disallows symlinks
	 * @param config
	 * @param callback
	 * @throws Exception
	 */
	public void scan(ScanConfig config, ScanCallback callback) throws Exception {
		String requestPath=normalizeMountPoint(config.getBaseDir());	// May be null (root)
		
		// Iterate over all mount points that share the given prefix
		for (Map.Entry<String,AssetMount> mountEntry: mountPoints.entrySet()) {
			String mountPoint=mountEntry.getKey();
			AssetMount mount=mountEntry.getValue();
			
			// We are actually looking for mount points that may include prefix,
			// so the sense of the following checks is inverted from what may be
			// expected
			// Note that the "/" mount point is null which makes this trickier
			// (why oh why did I do that to myself)
			if (requestPath==null || mountPoint==null || mountPoint.startsWith(requestPath)) {
				// The mount may contain the prefix
				String localPath;
				// Determine the part of requestPath that is local to the mount
				// and split
				if (mountPoint==null) localPath=requestPath;
				else if (requestPath==null) localPath=null;
				else {
					localPath=requestPath.substring(mountPoint.length());
					if (localPath.isEmpty()) localPath=null;
				}

				AssetPath assetPath=new AssetPath(mount, mountPoint!=null ? mountPoint : "", localPath!=null ? localPath : "");
				scanMount(assetPath, config, callback);
			}
		}
	}

	private void scanMount(AssetPath assetPath, ScanConfig config,
			ScanCallback callback) throws Exception {
		// Stat the path and decide what to do
		ResourceStat stat=assetPath.getMount().stat(assetPath);
		if (stat.isDirectory) {
			// Traverse the directory
			scanDirectory(assetPath, config, callback);
		} else {
			// Process the resource
			scanResource(assetPath, config, stat.associatedResources, callback);
		}
	}

	private void scanDirectory(AssetPath parentPath, ScanConfig config,
			ScanCallback callback) throws Exception {
		// Skip resources that are overlapped by another more specific resource
		// in another mount
		if (overlapped(parentPath)) return;
		if (!callback.handleDirectory(parentPath)) return;
		
		Collection<ResourceStat> children=parentPath.getMount().listChildren(parentPath);
		for (ResourceStat child: children) {
			if (child.isDirectory) {
				// Recusrive scan
				scanDirectory(child.path, config, callback);
			} else {
				// Handle the resource
				scanResource(child.path, config, child.associatedResources, callback);
			}
		}
	}

	private void scanResource(AssetPath assetPath, ScanConfig config,
			Collection<AssetPath> associatedResources, ScanCallback callback) throws Exception {
		// Skip resources that are overlapped by another more specific resource
		// in another mount
		if (overlapped(assetPath)) return;
		
		// Handle the root resource
		if (callback.handleAsset(assetPath)) {
			// Handle associated resources
			if (associatedResources!=null) {
				for (AssetPath assoc: associatedResources) {
					callback.handleAsset(assoc);
				}
			}
		}
	}
}
