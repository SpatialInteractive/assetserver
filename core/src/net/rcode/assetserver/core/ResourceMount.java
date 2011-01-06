package net.rcode.assetserver.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import net.rcode.assetserver.cache.Cache;
import net.rcode.assetserver.cache.CacheDependency;
import net.rcode.assetserver.cache.CacheEntry;
import net.rcode.assetserver.cache.CacheIdentity;
import net.rcode.assetserver.cache.FileCacheDependency;
import net.rcode.assetserver.util.IOUtil;
import net.rcode.assetserver.util.NamePattern;

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
	 * The owning server instance
	 */
	private AssetServer server;
	
	public ResourceMount(File location, AssetServer server) throws IOException {
		this.server=server;
		this.location=location.getCanonicalFile();
	}
	
	public AssetServer getServer() {
		return server;
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
		
		// See if we have a valid hit in the cache
		CacheIdentity identity=new CacheIdentity(getClass().getName(),
				assetPath.getMountPoint(), assetPath.getPath(),
				"");	// TODO: Add environment options redux
		Cache cache=server.getSharedCache();
		CacheEntry cacheEntry;
		if (cache!=null) {
			cacheEntry=cache.lookup(identity);
			if (cacheEntry!=null && cacheEntry.isValid()) {
				if (cacheEntry.isNullContent()) return null;
				else return cacheEntry;
			}
		}
		
		// No hit -
		// Create the root locator and initialize the filter chain
		MimeMapping mimeMapping=server.getMimeMapping();
		String mimeType=mimeMapping.lookup(resolvedFile.getName());
		FileAssetLocator rootLocator=new FileAssetLocator(resolvedFile);
		rootLocator.setContentType(mimeType);
		if (mimeMapping.isTextualMimeType(mimeType)) {
			// Set encoding
			rootLocator.setCharacterEncoding(server.getDefaultTextFileEncoding());
		}
		
		FilterChain chain=new FilterChain(server, assetPath, rootLocator, resolvedFile);
		chain.getDependencies().add(new FileCacheDependency(resolvedFile));
		initializeFilterChain(server.getContextManager().getRootContext(), chain, resolvedFile);
		chain.processFilters();
		
		// Get the resolved locator and handle caching
		AssetLocator resolvedLocator=chain.getCurrent();
		if (resolvedLocator==null || resolvedLocator.shouldCache()) {
			// Cache negative results or things explicitly cacheable
			CacheDependency[] dependencies=chain.getDependencies().toArray(new CacheDependency[chain.getDependencies().size()]);
			if (resolvedLocator==null) {
				// Store a negative cache entry
				cacheEntry=new CacheEntry(identity, dependencies, null, null, null);
				cache.store(cacheEntry);
				return null;
			} else {
				// Store a positive cache entry and return it
				cacheEntry=new CacheEntry(identity, dependencies, resolvedLocator.getContentType(),
						resolvedLocator.getCharacterEncoding(), slurpLocatorContents(resolvedLocator));
				cache.store(cacheEntry);
				return cacheEntry;
			}
		} else {
			// Not cacheable - just return
			return resolvedLocator;
		}
	}

	private byte[] slurpLocatorContents(AssetLocator resolvedLocator) throws IOException {
		InputStream input=resolvedLocator.openInput();
		return IOUtil.slurpBinary(input, (int)resolvedLocator.length());
	}

	protected void initializeFilterChain(ResourceContext resourceContext, FilterChain chain, File resolvedFile) {
		AssetPath assetPath=chain.getAssetPath();
		for (ResourceContext.FilterBinding binding: resourceContext.getFilters()) {
			if (binding.predicate.matches(assetPath)) {
				// Add to the chain
				binding.initializer.initializeChain(chain);
			}
		}
	}
	
	public String toString() {
		return "ResourceMount(" + location + ")";
	}
	

}
