package net.rcode.assetserver.standalone;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class DefaultFormatter extends Formatter {

	@Override
	public String format(LogRecord record) {
		String ret=record.getLevel().getName() +
			" (" + record.getLoggerName() + "): " +
			record.getMessage() + 
			"\n";
		
		Throwable thrown=record.getThrown();
		if (thrown!=null) {
			StringWriter sout=new StringWriter(512);
			PrintWriter out=new PrintWriter(sout);
			thrown.printStackTrace(out);
			out.flush();
			ret+=sout.toString() + "\n";
		}
		
		return ret;
	}

}
