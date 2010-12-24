package net.rcode.assetserver.standalone;

import java.io.PrintStream;

/**
 * Implementations define valid commands.
 * 
 * @author stella
 *
 */
public abstract class MainCommand {

	/**
	 * Override to print usage
	 */
	public boolean usage(PrintStream out) {
		return false;
	}
	
	/**
	 * Invoke the command
	 * @param args
	 * @throws Throwable
	 */
	public abstract void invoke(String[] args) throws Throwable;
}
