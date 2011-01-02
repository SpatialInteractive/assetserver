package net.rcode.assetserver.core;

import net.rcode.assetserver.util.NamePattern;

public class NamePatternPredicate implements AssetPredicate{
	private NamePattern pattern;
	
	public NamePatternPredicate(NamePattern pattern) {
		this.pattern=pattern;
	}

	@Override
	public boolean matches(AssetPath assetPath) {
		return pattern.matches(assetPath.getBaseName());
	}
	
}
