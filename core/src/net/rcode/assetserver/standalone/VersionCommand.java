package net.rcode.assetserver.standalone;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Properties;

import net.rcode.assetserver.util.IOUtil;

public class VersionCommand extends MainCommand {
	public static final String DESCRIPTION="Show version, copyright and license";
	
	@Override
	public boolean usage(PrintStream out) {
		out.println("Print version information");
		return true;
	}
	
	@Override
	public void invoke(String[] args) throws Throwable {
		// Load build properties
		Properties buildProps=new Properties();
		InputStream propIn=getClass().getResourceAsStream("/net/rcode/assetserver/buildinfo.properties");
		if (propIn!=null) buildProps.load(propIn);
		
		PrintWriter out=new PrintWriter(new OutputStreamWriter(System.err, "UTF-8"));
		out.println("assetserver, version " + buildProps.getProperty("build.version", "<unknown>"));
		out.println("Built at " + buildProps.getProperty("build.timestamp", "<unknown>") + 
				" on " + buildProps.getProperty("build.host", "<unknown>") +
				" by " + buildProps.getProperty("build.user", "<unknown>"));
		
		out.println(IOUtil.slurpResource(getClass(), "version.txt"));
		
		out.println();
		
		out.flush();
	}

}
