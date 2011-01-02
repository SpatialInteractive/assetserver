package net.rcode.assetserver.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Match against various name patterns.  Converts glob based name matches to
 * regular expressions.  Supports '*' and '?' characters.
 * <p>
 * The DEFAULT_EXCLUDES pattern contains a list of file names that should be
 * excludes from systematic processing.  This list mirrors Ant's exclusions, which
 * turn out to be pretty sane:
 * http://ant.apache.org/manual/dirtasks.html#defaultexcludes
 * 
 * @author stella
 *
 */
public class NamePattern {
	public static final NamePattern DEFAULT_EXCLUDES;
	private static final String[] DEFAULT_EXCLUDE_NAMES=new String[] {
		"*~",
		"#*#",
		".#*",
		"%*%",
		"._*",
		"CVS",
		".cvsignore",
		"SCCS",
		"vssver.scc",
		".svn",
		".DS_Store",
		".git",
		".gitattributes",
		".gitignore",
		".gitmodules",
		".hg",
		".hgignore",
		".hgsub",
		".hgsubstate",
		".hgtags",
		".bzr",
		".bzrignore"
	};
	
	private static final Pattern GLOB_SPLIT=Pattern.compile("\\*|\\?", Pattern.MULTILINE);
	
	private volatile Pattern matchExpression;
	private List<String> rawClauses=new ArrayList<String>();
	private boolean frozen;
	
	/**
	 * 
	 * @param name
	 * @return true if the name contains characters that should be interpreted by an instance
	 * of this class (instead of as a literal)
	 */
	public static boolean containsMetaChars(String name) {
		return GLOB_SPLIT.matcher(name).find();
	}
	
	public NamePattern() {
	}
	
	public NamePattern(String pattern) {
		include(pattern);
	}
	
	public NamePattern(String... patterns) {
		for (String pattern: patterns) {
			include(pattern);
		}
	}

	public NamePattern freeze() {
		frozen=true;
		compilePattern();
		return this;
	}
	
	public void include(String pattern) {
		if (frozen) throw new IllegalStateException("NamePattern is frozen");
		
		// Iterate over GLOB_SPLIT, replacing each glob with an expression and
		// quoting the other bits
		StringBuilder expression=new StringBuilder(pattern.length()*2);
		Matcher m=GLOB_SPLIT.matcher(pattern);
		int index=0;
		while (m.find(index)) {
			expression.append(Pattern.quote(pattern.substring(index, m.start())));
			String match=m.group();
			if (match.charAt(0)=='*') expression.append(".*");
			else if (match.charAt(0)=='?') expression.append(".");
			index=m.end();
		}
		
		// Terminal
		expression.append(pattern.substring(index));
		rawClauses.add(expression.toString());
		matchExpression=null;
	}
	
	public boolean matches(String name) {
		Pattern pattern=compilePattern();
		if (pattern==null) return false;
		else return pattern.matcher(name).matches();
	}

	private Pattern compilePattern() {
		Pattern ret=matchExpression;
		if (ret!=null) return ret;
		if (rawClauses.isEmpty()) return null;
		StringBuilder s=new StringBuilder();
		boolean first=true;
		s.append('^');
		for (String clause: rawClauses) {
			if (first) first=false;
			else s.append('|');
			s.append(clause);
		}
		s.append('$');
		
		ret=Pattern.compile(s.toString());
		matchExpression=ret;
		return ret;
	}
	
	public Pattern getPattern() {
		return compilePattern();
	}
	
	static {
		DEFAULT_EXCLUDES=new NamePattern(
				DEFAULT_EXCLUDE_NAMES
				);
		DEFAULT_EXCLUDES.freeze();
	}
}
