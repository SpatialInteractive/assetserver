package net.rcode.assetserver.standalone;

import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Level;

import net.rcode.assetserver.core.AssetServer;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	public JettyServer(AssetServer server) {
		this.server=server;
		this.jettyHandler=new JettyHandler(server);
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
		
		// Turn down logging, which jetty configures just after it inits
		java.util.logging.Logger.getLogger("org.eclipse.jetty").setLevel(Level.WARNING);
		
		jettyServer.start();
	}
	
	public void join() throws Exception {
		jettyServer.join();
	}
}
