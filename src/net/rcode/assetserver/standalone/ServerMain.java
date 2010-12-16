package net.rcode.assetserver.standalone;

import java.io.File;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import net.rcode.assetserver.core.AssetRoot;
import net.rcode.assetserver.core.ResourceMount;

import org.eclipse.jetty.server.Server;

/**
 * Main executable for starting a development server
 * @author stella
 *
 */
public class ServerMain {
	private PrintWriter console;
	private InetAddress listenAddress;
	private int httpPort=4000;
	private Server server;
	
	public ServerMain() {
		console=new PrintWriter(System.out, true);
	}
	
	public void setConsole(PrintWriter console) {
		this.console = console;
	}
	public PrintWriter getConsole() {
		return console;
	}
	
	public void start() throws Exception {
		// Figure listen address
		InetSocketAddress sa;
		if (listenAddress==null) sa=new InetSocketAddress(httpPort);
		else sa=new InetSocketAddress(listenAddress, httpPort);
		console.format("Starting assetserver on %s\n", sa);
		
		// Instantiate
		server=new Server(sa);
		
		// Configure handlers
		AssetRoot root=new AssetRoot();
		ResourceMount rootMount=new ResourceMount(new File("."));
		root.add("/", rootMount);
		
		AssetServerHandler handler=new AssetServerHandler();
		handler.setRoot(root);
		server.setHandler(handler);
		
		// Start
		server.start();
	}
	
	public void join() throws InterruptedException {
		server.join();
	}
	
	public static void main(String[] args) throws Exception {
		ServerMain server=new ServerMain();
		try {
			server.start();
		} catch (BindException e) {
			server.getConsole().format("FATAL: Unable to bind socket: %s\n", e.getMessage());
			System.exit(100);
		}
		server.join();
	}
}