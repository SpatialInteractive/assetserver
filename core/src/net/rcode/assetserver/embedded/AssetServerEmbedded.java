package net.rcode.assetserver.embedded;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import net.rcode.assetserver.core.AssetLocator;
import net.rcode.assetserver.core.AssetServer;
import net.rcode.assetserver.core.ScanCallback;
import net.rcode.assetserver.core.ScanConfig;
import net.rcode.cphelp.LoaderLookup;

/**
 * Helper class for instantiating an assetserver instance
 * in an embedded context.
 * @author stella
 *
 */
public class AssetServerEmbedded {
	private AssetServer server;
	
	public AssetServerEmbedded(File location) throws Exception {
		configureClassLoaders();
		server=new AssetServer(location);
	}

	/**
	 * Resolves the asset locator
	 * @param uri
	 * @return
	 * @throws Exception
	 */
	public AssetLocator resolve(String uri) throws Exception {
		server.enterRequestContext();
		try {
			return server.getRoot().resolve(uri);
		} finally {
			server.exitRequestContext();
		}
	}

	/**
	 * Run the scan method on the root
	 * @param config
	 * @param callback
	 * @throws Exception
	 */
	public void scan(ScanConfig config, ScanCallback callback) throws Exception {
		server.enterRequestContext();
		try {
			server.getRoot().scan(config, callback);
		} finally {
			server.exitRequestContext();
		}
	}
	
	public AssetServer getServer() {
		return server;
	}
	
	public static void configureClassLoaders() {
		if (!LoaderLookup.isInitialized()) {
			LoaderLookup.initialize(loaderProperties());
		}
	}
	
	private static Properties loaderProperties() {
		Properties ret=new Properties();
		InputStream stream=AssetServerEmbedded.class.getResourceAsStream("loader.properties");
		if (stream==null) throw new RuntimeException("Missing resource");
		try {
			try {
				ret.load(stream);
			} finally {
				stream.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return ret;
	}
}
