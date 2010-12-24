package net.rcode.assetserver.standalone;

import java.io.PrintWriter;

public class HelpCommand extends MainCommand {
	public static final String DESCRIPTION="Print this help page";
	
	@Override
	public void invoke(String[] args) throws Throwable {
		if (args.length==0) {
			// Print main help
			Main.overallUsage(null);
		} else {
			MainCommand command=(MainCommand) Main.getCommandClass(args[0]).newInstance();
			PrintWriter out=new PrintWriter(System.err, true);
			out.println("Help for command '" + args[0] + "'");
			if (!command.usage(out)) {
				out.println("No help available");
			}
		}
		System.exit(1);
	}

}
