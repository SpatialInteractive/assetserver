package net.rcode.assetserver.standalone;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Implementations define valid commands.
 * 
 * @author stella
 *
 */
public abstract class MainCommand {

	public void syntaxError(String message) throws IOException {
		PrintWriter out=new PrintWriter(System.err);
		out.println("Syntax error: " + message);
		out.println();
		
		usage(out);
		out.flush();
		
		System.exit(1);
	}
	
	/**
	 * Override to print usage
	 * @throws IOException 
	 */
	public boolean usage(PrintWriter out) throws IOException {
		return false;
	}
	
	/**
	 * Invoke the command
	 * @param args
	 * @throws Throwable
	 */
	public abstract void invoke(String[] args) throws Throwable;
}
