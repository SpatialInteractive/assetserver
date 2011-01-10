package net.rcode.assetserver.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Some utility methods for manipulating paths.
 * 
 * @author stella
 *
 */
public class PathUtil {
	/**
	 * Split pattern of "/"
	 */
	public static final Pattern PATH_SPLIT_PATTERN=Pattern.compile("\\/");
	
	/**
	 * Empty array of strings
	 */
	public static final String[] EMPTY_STRINGS=new String[0];
	
	/**
	 * Split the path by sep components (/), ignoring leading and trailing slashes.
	 * Append the components to list.
	 * @param path
	 */
	public static String[] splitPath(String path) {
		path=trimSlashes(path);
		if (path.isEmpty()) return EMPTY_STRINGS;
		
		return PATH_SPLIT_PATTERN.split(path);
	}
	
	/**
	 * Trim leading and trailing slashes from the string, returning the altered string
	 * @param path
	 * @return trimmed path
	 */
	public static String trimSlashes(String path) {
		if (path.startsWith("/")) path=path.substring(1);
		if (path.endsWith("/")) path=path.substring(0, path.length()-1);
		return path;
	}

	/**
	 * Equivilent to normalizePath(...) but returns the resultant path as components
	 * instead of joining them.
	 * @param relativeTo
	 * @param path
	 * @return list of path components
	 */
	public static List<String> normalizePathComponents(String relativeTo, String path) {
		String[] components=splitPath(path);
		ArrayList<String> normalizedComponents=new ArrayList<String>(components.length+10);
		if (!path.startsWith("/")) {
			// It is not absolute.  Append the root components.
			normalizedComponents.addAll(Arrays.asList(splitPath(relativeTo)));
		}
		
		// Split the path and process each component
		for (String component: components) {
			if (component.isEmpty()) {
				// Just a duplicate slash which happens in sloppy string manipulation.  Ignore
				continue;
			}
			
			if (".".equals(component)) {
				// Reference to the current component.  Continue
				continue;
			}
			
			if ("..".equals(component)) {
				// Step up one level
				if (normalizedComponents.isEmpty()) {
					// Proceeded up past the root
					return null;
				} else {
					// Pop the last one
					normalizedComponents.remove(normalizedComponents.size()-1);
					continue;
				}
			}
			
			// It's just a normal component
			normalizedComponents.add(component);
		}
		
		return normalizedComponents;
	}
	
	/**
	 * If path is not absolute (does not start with a leading /), then compute
	 * it relative to rootPath.  In all cases, "." and ".." path segments are
	 * expanded in path (but not rootPath).
	 * 
	 * @param relativeTo
	 * @param path
	 * @return normalized path or null if the path is not valid
	 */
	public static String normalizePath(String relativeTo, String path) {
		// Join it back together and return
		List<String> components=normalizePathComponents(relativeTo, path);
		if (components==null) return null;
		return joinPath(components);
	}
	
	/**
	 * Takes a list of components and returns a joined path.  If the list is empty,
	 * return "/".  The path will always begin with a slash and never end with one
	 * unless if it is the root path.
	 * 
	 * @param components
	 * @return joined path
	 */
	public static String joinPath(List<String> components) {
		if (components.isEmpty()) return "/";
		
		StringBuilder builder=new StringBuilder();
		for (String component: components) {
			builder.append('/');
			builder.append(component);
		}
		
		return builder.toString();
	}
	
	/**
	 * Vararg version of joinPath
	 * @param components
	 * @return joined path
	 */
	public static String joinPath(String... components) {
		return joinPath(Arrays.asList(components));
	}
	
	/**
	 * Construct a relative path from the given components
	 * @param components
	 * @return relative path
	 */
	public static String joinRelativePath(List<String> components) {
		StringBuilder builder=new StringBuilder();
		for (String component: components) {
			if (builder.length()>0) builder.append('/');
			builder.append(component);
		}
		
		return builder.toString();
	}
	
	/**
	 * Vararg version of joinRelativePath
	 * @param components
	 * @return relative path
	 */
	public static String joinRelativePath(String... components) {
		return joinRelativePath(Arrays.asList(components));
	}
	
	/**
	 * Return the dirname of the path.  This basically just extracts the substring
	 * from the beginning to the last slash (including the last slash) or '/'
	 * if no slash.
	 * @param path
	 * @return dirname
	 */
	public static String dirname(String path) {
		int slashPos=path.lastIndexOf('/');
		if (slashPos<0) return "/";
		return path.substring(0, slashPos+1);
	}
	
	/**
	 * Given path, relative to fromPath, translate it to a path relative to
	 * toPath using ".." components as needed.
	 *  
	 * @param fromPath
	 * @param toPath
	 * @param path
	 * @return translated relative path, or null if cannot be translated
	 */
	public static String translateRelative(String fromPath, String toPath, String path) {
		// First, normalize fromPath/path
		List<String> fromComponents=normalizePathComponents(fromPath, path);
		if (fromComponents==null) return null;
		
		// Explode the toPath
		String[] toComponents=splitPath(toPath);
		List<String> targetComponents=new ArrayList<String>(fromComponents.size()+10);
		
		// Identify the common prefix, if any
		int minLength=Math.min(fromComponents.size(), toComponents.length);
		int suffixIndex=0;
		for (int i=0; i<minLength; i++) {
			if (!toComponents[i].equals(fromComponents.get(i))) break;
			suffixIndex++;
		}
		
		// suffixIndex now points to the first non-equal component in both lists
		// any remaining components in toComponents must be expanded as ".."
		for (int i=suffixIndex; i<toComponents.length; i++) {
			targetComponents.add("..");
		}
		
		// and then all remaining components of fromComponents must be added
		for (int i=suffixIndex; i<fromComponents.size(); i++) {
			targetComponents.add(fromComponents.get(i));
		}
		
		// then join the path together as a relative path
		return joinRelativePath(targetComponents);
	}
}
