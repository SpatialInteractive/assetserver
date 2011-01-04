package net.rcode.assetserver.util;

import java.util.regex.Pattern;

/**
 * Constructs a pattern matcher against an entire path.
 * 
 * @author stella
 *
 */
public class PathPattern {
	/**
	 * Split pattern based on slash
	 */
	private static final Pattern SLASH_PATTERN=Pattern.compile("/");
	
	/**
	 * We canonicalize the "deep-star" match to this value on parse so we can do a simple
	 * comparison
	 */
	private static final String M_DEEPSTAR="**";
	
	/**
	 * If encountering this value, then the actual component is a Pattern in the matchAux array
	 */
	private static final String M_NAMEPATTERN="<NAMEPATTERN>";
	
	private String[] matchComponents;
	private Object[] matchAux;
	private NamePattern excludes=NamePattern.DEFAULT_EXCLUDES;
	
	public PathPattern(String pattern) {
		this(SLASH_PATTERN.split(trimSlashes(pattern)));
	}
	
	public PathPattern(String[] patternComponents) {
		matchComponents=patternComponents.clone();
		matchAux=new Object[matchComponents.length];
		for (int i=0; i<matchComponents.length; i++) {
			String comp=matchComponents[i];
			
			// Replace the component if it has special meaning.  Otherwise, just leave it as a literal match
			if (comp.equals(M_DEEPSTAR)) matchComponents[i]=M_DEEPSTAR;
			else if (NamePattern.containsMetaChars(comp)) {
				// It is a name pattern
				matchComponents[i]=M_NAMEPATTERN;
				NamePattern np=new NamePattern(comp);
				matchAux[i]=np.getPattern();
			}
		}
	}
	
	/**
	 * 
	 * @param pattern
	 * @return true if the pattern represents a path (contains slashes).  Otherwise, treat as
	 * a name pattern
	 */
	public static boolean isPathPattern(String pattern) {
		return pattern.indexOf('/')>=0;
	}
	
	/**
	 * Convenience method for splitting a path into components
	 * @param input
	 * @return array of components
	 */
	public static String[] splitComponents(String input) {
		input=trimSlashes(input);
		return SLASH_PATTERN.split(input);
	}
	
	/**
	 * Return the length of components that form the prefix of the pattern.
	 * The prefix is the part of the pattern that contain only literal
	 * components.
	 * 
	 * @param components
	 * @return number of components at the start of the array that form the prefix
	 */
	public static int findPrefix(String[] components) {
		for (int i=0; i<components.length; i++) {
			if (!isLiteralComponents(components[i])) {
				return i;
			}
		}
		
		return components.length;
	}
	
	/**
	 * @return true if the given path component has no special meaning and should be
	 * considered a literal
	 */
	public static boolean isLiteralComponents(String component) {
		return !component.equals("**") && !NamePattern.containsMetaChars(component);
	}

	/**
	 * Join path components together such that if there are any components specified,
	 * the resulting path will start with a slash but never end with a slash.
	 * 
	 * @param components
	 * @return joined path
	 */
	public static String joinComponents(String[] components, int from, int length) {
		StringBuilder sb=new StringBuilder(256);
		for (int i=from; i<(length+from); i++) {
			String c=components[i];
			sb.append('/');
			sb.append(c);
		}
		
		return sb.toString();
	}
	
	public NamePattern getExcludes() {
		return excludes;
	}
	
	public void setExcludes(NamePattern excludes) {
		this.excludes = excludes;
	}
	
	private static String trimSlashes(String in) {
		if (in.startsWith("/")) in=in.substring(1);
		if (in.endsWith("/")) in=in.substring(0, in.length()-1);
		return in;
	}
	
	/**
	 * @param path
	 * @return true if the given path matches the pattern
	 */
	public boolean matches(String path) {
		path=trimSlashes(path);
		String[] pathComponents=SLASH_PATTERN.split(path);
		return matchesHelp(pathComponents, 0, 0);
	}
	
	private boolean matchesHelp(String[] pathComponents, int pathIndex, int matchIndex) {
		for (;;) {
			String matchComp=matchComponents[matchIndex];
			String pathComp=pathComponents[pathIndex];
			
			// Check excludes
			if (excludes!=null && excludes.matches(pathComp)) {
				return false;
			}
			
			// Switch based on special matchComps
			if (matchComp==M_NAMEPATTERN) {
				// Apply the regular expression
				Pattern pattern=(Pattern) matchAux[matchIndex];
				if (!pattern.matcher(pathComp).matches()) return false;
			} else if (matchComp==M_DEEPSTAR) {
				// Start a recursive match
				return matchesDeepStar(pathComponents, pathIndex, matchIndex+1);
			} else {
				// Literal
				if (!matchComp.equals(pathComp)) return false;
			}
			
			// If falling through, then we increment to the next component
			pathIndex+=1;
			matchIndex+=1;
			
			// If both wrap off the end, then we are fully matched
			if (pathIndex==pathComponents.length) {
				if (matchIndex==matchComponents.length) return true;
				else return false;
			}
			if (matchIndex==matchComponents.length) return false;
			
			// Loop
			continue;
		}
	}

	/**
	 * Recursively match a deep star.  This method can certainly be optimized but I'm just
	 * going to make it work first
	 * 
	 * @param pathComponents path components we are matching against
	 * @param pathIndex the index in the path components we are matching against
	 * @param matchIndex The first matchIndex after the deep star
	 * @return true if it matches
	 */
	private boolean matchesDeepStar(String[] pathComponents, int pathIndex, int matchIndex) {
		if (matchIndex==matchComponents.length) {
			// The deep star was the last component.  This is a valid match since it matched
			// itself
			return true;
		}
		
		// At this point, we should trivially reject on a number of cases but my brain hurts
		// and I'm not going to optimize this right now.  Naive recursive descent, baby!
		for (;;) {
			if (matchesHelp(pathComponents, pathIndex, matchIndex)) return true;
			
			// No match, increment pathIndex and try again
			pathIndex+=1;
			if (pathIndex==pathComponents.length) {
				// Short pattern
				return false;
			}
		}
	}
}
