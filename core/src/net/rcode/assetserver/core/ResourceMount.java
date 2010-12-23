package net.rcode.assetserver.core;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Serves assets off of a directory on the file system.  This also serves
 * as a baseclass for asset mounts that process the results to generate
 * the output.
 * <p>
 * This class also imposes a syntax on paths, allowing an argument list.  The
 * last path component can have a syntax like:
 * <pre>
 *    /cdn/loader$profile=mobile$.js
 * </pre>
 * If this pattern is found in the last component, then the filename is rewritten
 * to loader.js and the parameter string is passed down to the resource handler for
 * the type.
 * <p>
 * Each component of the path is separated by a forward slash and the contents url decoded
 * to match a file on the file system.  A number of characters are illegal in file
 * components and will cause the path to be rejected.  The following characters are illegal:
 * <pre>
 * 	/\:
 * </pre>
 * In addition, no path component can be ".." or "." to avoid directory traversal attacks.
 * <p>
 * Prior to matching a resource, the file is located on the filesystem and compared to
 * its canonical path in order to work around case insesntive file systems.
 * 
 * @author stella
 *
 */
public class ResourceMount extends AssetMount {
	/**
	 * Name patterns that will be excluded by default if encountered
	 */
	private NamePattern defaultExclusions=NamePattern.DEFAULT_EXCLUDES;
	
	/**
	 * Name patterns that will be excluded
	 */
	private NamePattern userExclusions;
	
	/**
	 * The root location of this mount (canonicalized)
	 */
	private File location;
	
	/**
	 * List of handlers to consult on how to serve a file.  LIFO order.
	 */
	private LinkedList<HandlerEntry> handlers=new LinkedList<ResourceMount.HandlerEntry>();
	
	private static class HandlerEntry {
		public NamePattern pattern;
		public ResourceHandler handler;
		
		public HandlerEntry(NamePattern pattern, ResourceHandler handler) {
			this.pattern=pattern;
			this.handler=handler;
		}
	}
	
	public ResourceMount(File location) throws IOException {
		this.location=location.getCanonicalFile();
	}
	
	public File getLocation() {
		return location;
	}
	
	public NamePattern getDefaultExclusions() {
		return defaultExclusions;
	}
	public void setDefaultExclusions(NamePattern defaultExclusions) {
		this.defaultExclusions = defaultExclusions;
	}
	
	public NamePattern getUserExclusions() {
		return userExclusions;
	}
	public void setUserExclusions(NamePattern userExclusions) {
		this.userExclusions = userExclusions;
	}
	
	/**
	 * Add a handler for the given namePattern.  Handlers added later have precedence.
	 * @param namePattern
	 * @param handler
	 */
	public void addHandler(NamePattern namePattern, ResourceHandler handler) {
		handlers.addFirst(new HandlerEntry(namePattern, handler));
	}
	
	/**
	 * Add a handler that matches a single name glob.
	 * @param namePattern
	 * @param handler
	 */
	public void addHandler(String namePattern, ResourceHandler handler) {
		addHandler(new NamePattern(namePattern).freeze(), handler);
	}
	
	@Override
	public AssetLocator resolve(AssetPath assetPath) throws Exception {
		// Reconstruct the path using native directory separators so that we can
		// do a string compare with a canonical path in order to determine correctness
		// This will not work across symbolic links.
		StringBuilder pathAccum=new StringBuilder(location.toString().length() + assetPath.getPath().length() + 256);
		pathAccum.append(location.toString());
		for (String comp: assetPath.getPathComponents()) {
			if (pathAccum.charAt(pathAccum.length()-1)!=File.separatorChar)
				pathAccum.append(File.separatorChar);
			pathAccum.append(comp);
		}
		
		String resolvedPath=pathAccum.toString();
		File resolvedFile=new File(resolvedPath);
		String canonicalPath=resolvedFile.getCanonicalPath();
		if (!canonicalPath.equals(resolvedPath)) {
			// The path resolved differently.  If it is just a case issue, then the paths
			// are not the same
			if (canonicalPath.equalsIgnoreCase(resolvedPath)) {
				// Case mismatch
				return null;
			} else {
				// Potentially crosses a symlink boundary - do an expensive step-by-step eval
				// TODO
				return null;
			}
		}
		
		// Validate that none of the path components contain names that are excluded
		for (String comp: assetPath.getPathComponents()) {
			if (defaultExclusions!=null && defaultExclusions.matches(comp)) return null;
			if (userExclusions!=null && userExclusions.matches(comp)) return null;
		}
		
		// If here, then resolvedFile is indeed a valid canonical file.  The only other thing
		// to check is whether the file exists
		if (!resolvedFile.isFile()) {
			return null;
		}
		
		// Ok.  It's a valid file.  Find a handler for it.
		String baseName=assetPath.getBaseName();
		ResourceHandler handler=null;
		for (HandlerEntry entry: handlers) {
			if (entry.pattern.matches(baseName)) {
				// Found one
				handler=entry.handler;
				break;
			}
		}
		
		if (handler==null) return null;	// No handler
		
		AssetLocator locator=handler.accessResource(this, assetPath, resolvedFile);
		return locator;
	}
}
