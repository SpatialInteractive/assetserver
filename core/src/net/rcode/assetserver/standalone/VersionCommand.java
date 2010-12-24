package net.rcode.assetserver.standalone;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import net.rcode.assetserver.VersionInfo;
import net.rcode.assetserver.util.IOUtil;

public class VersionCommand extends MainCommand {
	public static final String DESCRIPTION="Show version, copyright and license";
	
	@Override
	public boolean usage(PrintWriter out) {
		out.println("Print version information");
		return true;
	}
	
	@Override
	public void invoke(String[] args) throws Throwable {
		VersionInfo version=VersionInfo.INSTANCE;
		
		PrintWriter out=new PrintWriter(new OutputStreamWriter(System.err, "UTF-8"));
		out.println("assetserver, version " + version.getBuildVersion());
		out.println("Built at " + version.getBuildTime() + 
				" on " + version.getBuildHost() +
				" by " + version.getBuildUser());
		
		out.println(IOUtil.slurpResource(getClass(), "version.txt"));
		
		out.println();
		
		out.flush();
	}

}
