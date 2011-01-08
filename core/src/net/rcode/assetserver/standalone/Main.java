package net.rcode.assetserver.standalone;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import net.rcode.assetserver.core.AssetServer;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Main class.  Does overall command line parsing and dispatch.
 * <p>
 * The assetserver command line is of the pattern:
 * <pre>
 *   assetserver [overall options] command [command options]
 * <pre>
 * 
 * 
 * @author stella
 *
 */
public class Main {
	private static boolean inited=false;
	private static final Map<String, String> commandClassNames=new TreeMap<String, String>();
	private static OptionParser overallParser;
	
	private static void initOnce() {
		if (inited) return;
		inited=true;
		
		// Add commands
		String CP="net.rcode.assetserver.standalone.";
		commandClassNames.put("help", CP + "HelpCommand");
		commandClassNames.put("version", CP + "VersionCommand");
		commandClassNames.put("serve", CP + "ServeCommand");
		commandClassNames.put("cp", CP + "CopyCommand");
		
		// Init option parser
		overallParser=new OptionParser();
	}
	
	public static void main(String[] args) throws Throwable {
		// Set AWT headless mode
		System.setProperty("java.awt.headless", "true");
		
		// Initialize the overall option parser
		initOnce();
		resetLogging();
		
		// First split the command line into overall, command and command options
		int i;
		for (i=0; i<args.length; i++) {
			if (args[i].startsWith("-")) continue;
			else break;
		}
		
		String commandStr, commandClassName;
		String[] overallArgs;
		String[] commandArgs;
		boolean showDefaultWarning=false;
		
		if (i>=args.length) {
			// There was no command given.  We default to starting the server for the
			// current directory.   
			commandStr="serve";
			overallArgs=sliceArray(args, 0, i);
			commandArgs=new String[] { "." };
			showDefaultWarning=true;	// Write a warning about defaults after logging initialized
		} else {
			commandStr=args[i];
			overallArgs=sliceArray(args, 0, i);
			commandArgs=sliceArray(args, i+1, args.length);
		}
		
		commandClassName=commandClassNames.get(commandStr);
		if (commandClassName==null) {
			overallUsage("Unrecognied command '" + commandStr + "'");
			System.exit(1);
		}
		
		
		// Parse the overall args
		OptionSet overallOptions;
		try {
			overallOptions=overallParser.parse(overallArgs);
		} catch (OptionException e) {
			overallUsage(e.getMessage());
			System.exit(1);
		}
		
		if (showDefaultWarning) {
			AssetServer.logger.info("No command given. Starting server out of current directory.  For help, run 'assetserver help'");
		}
		
		// Load the command class
		Class<?> commandClass=Class.forName(commandClassName, true, Main.class.getClassLoader());
		MainCommand command=(MainCommand) commandClass.newInstance();
		command.invoke(commandArgs);
	}

	/**
	 * Remove all handlers from the root logger
	 */
	private static void resetLogging() {
		LogManager logManager=LogManager.getLogManager();
		
		Logger logger=logManager.getLogger("");
		Handler[] handlers=logger.getHandlers();
		for (Handler handler: handlers) {
			logger.removeHandler(handler);
		}
		
		// Reset to warning level
		logger.setLevel(Level.INFO);
		ConsoleHandler handler=new ConsoleHandler();
		handler.setFormatter(new DefaultFormatter());
		logger.addHandler(handler);
	}

	static Class<?> getCommandClass(String commandName) throws ClassNotFoundException {
		String commandClassName=commandClassNames.get(commandName);
		if (commandClassName==null) return null;
		return Class.forName(commandClassName, true, Main.class.getClassLoader());
	}
	
	static String getCommandDescription(String commandName) {
		try {
			return (String) getCommandClass(commandName).getField("DESCRIPTION").get(null);
		} catch (Exception e) {
			return "";
		}
	}
	
	private static String[] sliceArray(String[] args, int start, int end) {
		if (start>=args.length) return new String[0];
		if (end>=args.length) end=args.length;
		String[] ret=new String[end-start];
		for (int i=start; i<end; i++) {
			ret[i-start]=args[i];
		}
		
		return ret;
	}

	static void overallUsage(String syntaxError) throws IOException {
		PrintStream out=System.err;
		if (syntaxError!=null) out.println("Syntax error: " + syntaxError);
		out.println("Usage: assetserver [overall options] command [command options]");
		out.println();
		out.println("Commands:");
		
		for (String commandName: commandClassNames.keySet()) {
			out.print("\t");
			out.print(commandName);
			out.print("\t\t");
			out.print(getCommandDescription(commandName));
			out.println();
		}
		
		out.println();
		out.println("Overall options:");
		overallParser.printHelpOn(out);
		out.println();
	}
}
