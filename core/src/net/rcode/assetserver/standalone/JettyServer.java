package net.rcode.assetserver.standalone;

import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Level;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rcode.assetserver.core.AssetServer;

/**
 * Adapts an AssetServer instance by adding http interfaces.
 * @author stella
 *
 */
public class JettyServer {
	private static final Logger logger=LoggerFactory.getLogger("httpserver");
	
	private AssetServer server;
	private JettyHandler jettyHandler;
	private int httpPort=8080;
	private InetAddress bindAddress;
	private Server jettyServer;
	
	static {
		// Jetty resets some logging config after class init.  Force the issue and
		// reset here
		Class<?> dummy=Server.class;
		// Set some specific loggers
		java.util.logging.Logger.getLogger("org.eclipse.jetty").setLevel(Level.WARNING);
	}
	
	public JettyServer(AssetServer server) {
		this.server=server;
		this.jettyHandler=new JettyHandler();
		this.jettyHandler.setRoot(server.getRoot());
	}
	
	public int getHttpPort() {
		return httpPort;
	}
	public void setHttpPort(int httpPort) {
		this.httpPort = httpPort;
	}
	
	public InetAddress getBindAddress() {
		return bindAddress;
	}
	public void setBindAddress(InetAddress bindAddress) {
		this.bindAddress = bindAddress;
	}
	
	public AssetServer getServer() {
		return server;
	}
	
	public void setServer(AssetServer server) {
		this.server = server;
	}
	
	public void start() throws Exception, BindException {
		// Figure listen address
		InetSocketAddress sa;
		if (bindAddress==null) {
			logger.info("Starting HTTP server on port " + httpPort + " (all addresses)");
			sa=new InetSocketAddress(httpPort);
		} else {
			logger.info("Starting HTTP server on port " + httpPort + " (" +bindAddress + ")");
			sa=new InetSocketAddress(bindAddress, httpPort);
		}
		
		jettyServer=new Server(sa);
		jettyServer.setHandler(jettyHandler);
		
		jettyServer.start();
	}
	
	public void join() throws Exception {
		jettyServer.join();
	}
}
