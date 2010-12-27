package net.rcode.assetserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Encapsualte access to the build time version properties
 * @author stella
 *
 */
public class VersionInfo {
	public static final VersionInfo INSTANCE=new VersionInfo();
	private Properties properties;
	
	VersionInfo() {
		properties=new Properties();
		try {
			InputStream propIn=getClass().getResourceAsStream("buildinfo.properties");
			try {
				if (propIn!=null) properties.load(propIn);
			} finally {
				if (propIn!=null) propIn.close();
			}
		} catch (IOException e) {
			// Ignore
		}
	}
	
	public String getBuildVersion() {
		return properties.getProperty("build.version", "<unknown>");
	}
	
	public String getBuildTime() {
		return properties.getProperty("build.timestamp", "<unknown>");
	}
	
	public String getBuildHost() {
		return properties.getProperty("build.host", "<unknown>");
	}
	
	public String getBuildUser() {
		return properties.getProperty("build.user", "<unknown>");
	}
}
