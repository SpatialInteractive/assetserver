package net.rcode.assetserver.core;

/**
 * When invoking the scan(...) method on AssetRoot, it takes one of these
 * to control scanning.
 * 
 * @author stella
 *
 */
public class ScanConfig {
	private String baseDir;
	private boolean recursive;
	
	public ScanConfig() {
	}
	
	public String getBaseDir() {
		return baseDir;
	}
	
	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}
	
	public boolean isRecursive() {
		return recursive;
	}
	
	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}
}
