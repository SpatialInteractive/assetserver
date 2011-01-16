package net.rcode.assetserver.standalone;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.rcode.assetserver.cache.NullCache;
import net.rcode.assetserver.core.AssetLocator;
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
	public static final String DESCRIPTION="Copy resources to the filesystem";
	
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
		final File toLocation=new File(arguments.get(arguments.size()-1));
		List<String> fromPaths=arguments.subList(1, arguments.size()-1);
		
		// Boot up the server
		File configLocation=new File(serverRoot);
		final AssetServer server=new AssetServer(configLocation);
		
		// Disable the cache
		server.setSharedCache(new NullCache());
		
		// Set up for scan
		ScanConfig config=new ScanConfig();
		final CopyProcessor processor=new CopyProcessor(server, toLocation);
		processor.verbosity=10;
		processor.verboseWriter=new PrintWriter(System.err, true);
		
		// Case 1. Two arguments and the from is a file and the to either does not exist or is a file
		// Support single resource copy.  This doesn't actually do a scan
		if (fromPaths.size()==1 && (!toLocation.exists() || toLocation.isFile())) {
			processor.copySingle(fromPaths.get(0));
			return;
		}
		
		// If no fromPaths, then this is an implicit copy of the entire tree
		if (fromPaths.isEmpty()) {
			fromPaths=Collections.singletonList("/");
		}
		
		// And process each fromPath
		if (!toLocation.isDirectory()) {
			fail("The target directory '" + toLocation + "' does not exist or is not a directory.");
			return;
		}

		for (String fromPath: fromPaths) {
			if (!fromPath.startsWith("/")) {
				// It is common form on the command line to not include the
				// leading slash but the scanner requires it
				fromPath='/' + fromPath;
			}
			config.setRecursive(true);
			
			if (processor.verbosity>=5) {
				processor.verboseWriter.println("Scanning " + fromPath);
			}
			
			boolean fromChildren=fromPath.endsWith("/");
			String prefix;
			if (fromChildren) {
				// The prefix is the fromPath
				prefix=fromPath;
				
				// Set the basedir without the trailing slash (reduces root to "")
				config.setBaseDir(fromPath.substring(0, fromPath.length()-1));

				if (processor.verbosity>=5) {
					processor.verboseWriter.println("Copying children of '" + config.getBaseDir() + "' (prefix=" + prefix + ")");
				}
			} else {
				// Does not end in a slash.  In this case, the prefix we are looking for
				// excludes the last path segment.  There will always be a slash because
				// we explicitly prefixed one above
				int slashPos=fromPath.lastIndexOf('/');
				prefix=fromPath.substring(0, slashPos+1);	// We want the prefix to include the slash
				
				// Set the basedir to the requested path
				config.setBaseDir(fromPath);
				
				if (processor.verbosity>=5) {
					processor.verboseWriter.println("Copying parent " + config.getBaseDir() + " (prefix=" + prefix + ")");
				}
			}
			
			processor.outputPrefix=prefix;
			
			// Initiate scan
			server.getRoot().scan(config, processor);
			processor.reportRuntime();
		}
	}
	
	private void fail(String msg) {
		System.err.println("ERROR: " + msg);
		System.exit(10);
	}

	private class CopyProcessor implements ScanCallback {
		public long startTime=System.currentTimeMillis();
		public File destination;
		public int verbosity;
		public PrintWriter verboseWriter;
		public String outputPrefix;
		private AssetServer server;
		
		public CopyProcessor(AssetServer server, File destination) {
			this.destination=destination;
			this.server=server;
		}

		/**
		 * Report writing a file: 1/3
		 * @param resource
		 */
		public void reportOpening(String resource) {
			if (verbosity>0) {
				verboseWriter.print("Opening '" + resource + "' ... ");
				verboseWriter.flush();
			}
		}
		
		/**
		 * Report writing a file: 2/3
		 * @param toLocation
		 */
		public void reportSaving(Object toLocation) {
			if (verbosity>0) {
				verboseWriter.print("Saving to '" + toLocation + "' ... ");
				verboseWriter.flush();
			}
		}
		
		/**
		 * Report writing a file: 3/3
		 */
		public void reportDoneSaving() {
			if (verbosity>0) {
				verboseWriter.println("Done");
			}
		}
		
		public void reportDirectory(String serverPath, Object localPath) {
			if (verbosity>0) {
				verboseWriter.println("Creating local directory " + localPath + " for " + serverPath);
			}
		}
		
		public void reportRuntime() {
			if (verbosity>=5) {
				verboseWriter.println("Finished operation in " + (((double)(System.currentTimeMillis()-startTime))/1000.0) + "s");
			}
		}
		
		public void copySingle(String singleFrom) throws Exception {
			server.enterRequestContext();
			try {
				// Look up the single named resource
				reportOpening(singleFrom);
				
				AssetLocator locator=server.getRoot().resolve(singleFrom);
				if (locator==null) {
					fail("From location '" + singleFrom + "' does not exist.");
					return;
				}
				
				// Delete and copy
				reportSaving(destination);
				IOUtil.interlockedWriteFile(destination, locator);
				
				reportDoneSaving();
				reportRuntime();
			} finally {
				server.exitRequestContext();
			}
		}
		
		@Override
		public boolean handleAsset(AssetPath path) throws Exception {
			server.enterRequestContext();
			try {
				String fullAssetPath=path.getFullParameterizedPath();
				File localPath=resolveLocal(fullAssetPath);
				
				if (localPath==null) {
					throw new IllegalStateException("INTERNAL ERROR: Scanner returned a path (" + fullAssetPath + ") not contained under the output prefix (" +
							outputPrefix + ")");
				}
				
				reportOpening(fullAssetPath);
				AssetLocator locator=path.getMount().resolve(path);
				reportSaving(localPath);
				
				localPath.getParentFile().mkdirs();
				IOUtil.interlockedWriteFile(localPath, locator);
				reportDoneSaving();
				
				return true;
			} finally {
				server.exitRequestContext();
			}
		}

		@Override
		public boolean handleDirectory(AssetPath path) throws Exception {
			server.enterRequestContext();
			try {
				String fullAssetPath=path.getFullParameterizedPath();
				if (fullAssetPath.isEmpty()) {
					// Do nothing if the root directory (happens on root mounts)
					return true;
				}
				
				File localPath=resolveLocal(fullAssetPath);
				if (localPath==null) {
					// This sometimes happens for directories because the parent directory will
					// be reported but is not under prefix.  Just ignore.
					return true;
				}
				
				reportDirectory(fullAssetPath, localPath);
				if (!localPath.mkdirs()) {
					if (!localPath.isDirectory()) {
						fail("Could not create directory " + localPath);
					}
				}
				
				return true;
			} finally {
				server.exitRequestContext();
			}
		}

		private File resolveLocal(String fullAssetPath) {
			if (!fullAssetPath.startsWith(outputPrefix)) {
				return null;
			}
			
			String localPath=fullAssetPath.substring(outputPrefix.length());
			
			return new File(destination, localPath);
		}
		
		
	}
	
}
