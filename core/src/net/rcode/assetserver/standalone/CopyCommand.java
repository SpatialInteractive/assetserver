package net.rcode.assetserver.standalone;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.rcode.assetserver.core.AssetPath;
import net.rcode.assetserver.core.AssetServer;
import net.rcode.assetserver.core.ScanCallback;
import net.rcode.assetserver.core.ScanConfig;
import net.rcode.assetserver.util.IOUtil;

/**
 * Implements the CLI cp command for copying from a server path to a filesystem
 * location.
 * 
 * @author stella
 *
 */
public class CopyCommand extends MainCommand {

	private OptionParser optionParser;
	
	public CopyCommand() {
		optionParser=new OptionParser();
		optionParser.posixlyCorrect(true);
	}
	
	@Override
	public boolean usage(PrintWriter out) throws IOException {
		out.println(IOUtil.slurpResource(getClass(), "copy.txt"));
		
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
		if (arguments.size()<2) {
			syntaxError("Insufficient number of arguments");
			return;
		}
		
		String serverRoot=arguments.get(0);
		String toLocation=arguments.get(arguments.size()-1);
		List<String> fromPaths=arguments.subList(1, arguments.size()-1);
		
		// Boot up the server
		File configLocation=new File(serverRoot);
		AssetServer server=new AssetServer(configLocation);
		
		ScanConfig config=new ScanConfig();
		if (fromPaths.isEmpty()) {
			// Trivial - copy entire namespace
			File targetDir=new File(toLocation);
			if (!targetDir.isDirectory()) {
				fail("The target directory '" + targetDir + "' does not exist.");
				return;
			}
			
			config.setBaseDir("/");
			config.setRecursive(true);
			
			server.getRoot().scan(config, new ScanCallback() {
				
				@Override
				public boolean handleDirectory(AssetPath path) throws Exception {
					System.out.println("SCAN: handleDirectory(" + path + ")");
					return true;
				}
				
				@Override
				public boolean handleAsset(AssetPath path)
						throws Exception {
					System.out.println("SCAN: handleAsset(" + path + ")");
					return true;
				}
			});
		}
	}
	
	private void fail(String msg) {
		System.err.println("ERROR: " + msg);
		System.exit(10);
	}

}
