package net.rcode.assetserver.standalone;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.rcode.assetserver.VersionInfo;
import net.rcode.assetserver.cache.NullCache;
import net.rcode.assetserver.core.AssetServer;
import net.rcode.assetserver.util.IOUtil;

public class ServeCommand extends MainCommand {
	public static final String DESCRIPTION="Run http server";
	
	private OptionParser optionParser;
	
	public ServeCommand() {
		optionParser=new OptionParser();
		optionParser.accepts("http", "Listen on http port")
			.withRequiredArg()
			.ofType(Integer.class)
			.defaultsTo(4080);
		optionParser.accepts("bind", "Bind to a specific network interface address (defaults to all addresses)")
			.withOptionalArg();
		optionParser.accepts("clear-cache", "Clear the cache prior to starting");
		optionParser.accepts("no-cache", "Disable the cache");
		optionParser.accepts("disable-optimization", "Disable optimization filters");
	}
	
	@Override
	public boolean usage(PrintWriter out) throws IOException {
		out.println("Usage: assetserver serve [options] serverroot");
		out.println();
		
		out.println(IOUtil.slurpResource(getClass(), "serve.txt"));

		out.println("Command options:");
		optionParser.printHelpOn(out);
		
		out.println();
		out.flush();
		
		return true;
	}
	
	@Override
	public void invoke(String[] args) throws Throwable {
		OptionSet optionSet;
		try {
			optionSet=optionParser.parse(args);
		} catch (OptionException e) {
			syntaxError(e.getMessage());
			return;
		}
		
		List<String> arguments=optionSet.nonOptionArguments();
		if (arguments.size()!=1) {
			syntaxError("Expected a single location");
			return;
		}

		// Print version banner
		VersionInfo version=VersionInfo.INSTANCE;
		AssetServer.logger.info("Starting assetserver version " + version.getBuildVersion() +
				" (built at " + version.getBuildTime() + " on " + version.getBuildHost() + " by " + version.getBuildUser() + ")");
		

		// Instantiate the server
		File configLocation=new File(arguments.get(0));
		AssetServer server=new AssetServer(configLocation);
		
		if (optionSet.has("no-cache")) {
			AssetServer.logger.info("Disabling cache");
			server.setSharedCache(new NullCache());
		} else {
			if (optionSet.has("clear-cache")) {
				AssetServer.logger.info("Clearing cache");
				server.getSharedCache().clear();
			}
		}
		
		if (optionSet.has("disable-optimization")) {
			server.setGlobalDisableOptimization(true);
		}
		
		AssetServer.logger.info("Configuration summary:\n" + server.summarizeConfiguration());
		
		JettyServer http=new JettyServer(server);
		Object bindAddress=optionSet.valueOf("bind");
		try {
			if (bindAddress!=null) http.setBindAddress(InetAddress.getByName(bindAddress.toString()));
		} catch (UnknownHostException e) {
			System.err.println("FATAL: Bind address '" + bindAddress + "' could not be resolved. " + e.getMessage());
			System.exit(3);
		}
		
		http.setHttpPort(((Integer)optionSet.valueOf("http")).intValue());
		
		try {
			http.start();
		} catch (BindException e) {
			System.err.println("FATAL: Cannot bind to the specified port and/or address. " + e.getMessage());
			System.exit(2);
		}
		
		http.join();
	}

}
