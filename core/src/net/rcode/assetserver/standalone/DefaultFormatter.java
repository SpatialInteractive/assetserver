package net.rcode.assetserver.standalone;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class DefaultFormatter extends Formatter {

	@Override
	public String format(LogRecord record) {
		return record.getLevel().getName() +
			" " + record.getMillis() +
			" (" + record.getLoggerName() + "): " +
			record.getMessage() + 
			"\n";
	}

}
