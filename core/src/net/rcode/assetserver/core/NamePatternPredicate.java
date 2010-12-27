package net.rcode.assetserver.core;

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
