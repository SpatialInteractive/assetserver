package net.rcode.assetserver.core;

import java.util.Arrays;

import net.rcode.assetserver.util.NamePattern;
import net.rcode.assetserver.util.PathPattern;

/**
 * An AssetPredicate that applies a pattern to the full path.
 * This predicate supports two types of patterns:
 * 
 * <h2>Name Pattern</h2>
 * If the pattern does not contain a slash it is deemed to be
 * a name pattern.  The pattern is interpreted with the NamePattern 
 * class which allows normal glob type patterns or literals.
 * 
 * <h2>Path Pattern</h2>
 * If the pattern contains a slash, it is interpreted by the
 * PathPattern class, which supports Ant-style patterns, including
 * the ** operator which matches the self or descendants.
 * Normal glob patterns are supported as well.
 * 
 * <h2>Prefix Patterns</h2>
 * If the pattern contains a slash but no other meta-characters,
 * then it is just matched against a literal prefix.
 * 
 * @author stella
 *
 */
public class PatternPredicateFactory {

	public static class NamePatternPredicate implements AssetPredicate {
		private String srcPattern;
		private NamePattern namePattern;
		
		public NamePatternPredicate(String pattern) {
			this.srcPattern=pattern;
			this.namePattern=new NamePattern(pattern);
		}
		
		@Override
		public boolean matches(AssetPath assetPath) {
			String name=assetPath.getBaseName();
			return namePattern.matches(name);
		}
		
		@Override
		public boolean equals(Object other) {
			if (other==null) return false;
			if (other instanceof NamePatternPredicate) {
				return srcPattern.equals(((NamePatternPredicate)other).srcPattern);
			}
			return false;
		}
		
		@Override
		public String toString() {
			return "NamePatternPredicate(" + srcPattern + ")";
		}
	}
	
	public static class NameLiteralPredicate implements AssetPredicate {
		private String nameLiteral;
		
		public NameLiteralPredicate(String nameLiteral) {
			this.nameLiteral=nameLiteral;
		}
		
		@Override
		public boolean matches(AssetPath assetPath) {
			String name=assetPath.getBaseName();
			return nameLiteral.equals(name);
		}
		
		@Override
		public boolean equals(Object other) {
			if (other==null) return false;
			if (other instanceof NameLiteralPredicate) {
				return nameLiteral.equals(((NameLiteralPredicate)other).nameLiteral);
			}
			return false;
		}
		
		@Override
		public String toString() {
			return "NameLiteralPredicate(" + nameLiteral + ")";
		}
	}
	
	public static class PathPatternPredicate implements AssetPredicate {
		private String[] components;
		private PathPattern pathPattern;
		
		public PathPatternPredicate(String[] components) {
			this.components=components;
			this.pathPattern=new PathPattern(components);
		}
		
		@Override
		public boolean matches(AssetPath assetPath) {
			String path=assetPath.getFullPath();
			return pathPattern.matches(path);
		}
		
		@Override
		public boolean equals(Object other) {
			if (other==null) return false;
			if (other instanceof PathPatternPredicate) {
				String[] rhsComponents=((PathPatternPredicate)other).components;
				return Arrays.equals(components, rhsComponents);
			}
			return false;
		}
		
		@Override
		public String toString() {
			return "PathPatternPredicate(" + Arrays.toString(components) + ")";
		}
	}

	public static class PrefixPatternPredicate implements AssetPredicate {
		private String[] components;
		private String prefix;
		private PathPattern pattern;
		
		public PrefixPatternPredicate(String[] components, int prefixLength) {
			this.components=components;
			this.prefix=PathPattern.joinComponents(components, 0, prefixLength);
			if (prefixLength<components.length) {
				// Would use Arrays.copyOfRange but still trying to hang onto Java 5 compatibility
				String[] patternComponents=new String[components.length - prefixLength];
				for (int i=0; i<prefixLength; i++) {
					patternComponents[i]=components[i+prefixLength];
				}
				this.pattern=new PathPattern(patternComponents);
				this.components=patternComponents;	// Override components
			}
		}
		
		@Override
		public boolean matches(AssetPath assetPath) {
			String path=assetPath.getFullPath();
			
			if (pattern==null) {
				// Prefix must match the whole thing
				return prefix.equals(path);
			} else {
				if (!path.startsWith(prefix)) return false;
				String subPath=path.substring(prefix.length());
				if (!subPath.startsWith("/")) return false;	// Can't just prefix into a component
				return pattern.matches(subPath);
			}
		}
		
		@Override
		public boolean equals(Object other) {
			if (other==null) return false;
			if (other instanceof PrefixPatternPredicate) {
				PrefixPatternPredicate rhs=(PrefixPatternPredicate)other;
				return Arrays.equals(components, rhs.components) &&
					prefix.equals(rhs.prefix);
			}
			return false;
		}
		
		@Override
		public String toString() {
			return "PrefixPatternPredicate(" + prefix + "," + Arrays.toString(components) + ")";
		}
	}
	
	/**
	 * Builds and returns an AssetPredicate suitable to best match the
	 * given pattern.
	 * @param pattern
	 * @return new AssetPredicate instance
	 */
	public static AssetPredicate build(String pattern) {
		// Path or name pattern?
		if (!PathPattern.isPathPattern(pattern)) {
			// Name or literal
			if (NamePattern.containsMetaChars(pattern)) {
				return new NamePatternPredicate(pattern);
			} else {
				return new NameLiteralPredicate(pattern);
			}
		}
		
		// If here, then a PathPredicate
		// Extract prefix
		String[] components=PathPattern.splitComponents(pattern);
		int prefixLength=PathPattern.findPrefix(components);
		if (prefixLength>0) {
			// Return a composite that matches prefix and pattern
			return new PrefixPatternPredicate(components, prefixLength);
		} else {
			// Return a simple PathPattern predicate
			return new PathPatternPredicate(components);
		}
	}
}
