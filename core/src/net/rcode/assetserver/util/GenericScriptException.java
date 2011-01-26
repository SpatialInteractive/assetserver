package net.rcode.assetserver.util;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;

import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.WrappedException;

/**
 * A non-platform exception representing a script error.
 * @author stella
 *
 */
public class GenericScriptException extends RuntimeException {
	private static final Pattern CLASS_NOSTACK_PATTERN=Pattern.compile("ShadowScope|GeneratorFunction|JettyHandler|reflect|mozilla");
	
	private String sourceName;
	private int sourceLineNumber;
	
	public GenericScriptException(String sourceName, int sourceLineNumber, String message) {
		super(message);
		this.sourceName=sourceName;
		this.sourceLineNumber=sourceLineNumber;
	}
	
	public int getSourceLineNumber() {
		return sourceLineNumber;
	}
	
	public String getSourceName() {
		return sourceName;
	}
	
	@Override
	public void printStackTrace(PrintStream s) {
		try {
			printStackTrace((Appendable)s);
		} catch (IOException e) { }
	}

	@Override
	public void printStackTrace(PrintWriter s) {
		try {
			printStackTrace((Appendable)s);
		} catch (IOException e) { }
	}

	
	public void printStackTrace(Appendable s) throws IOException {
		s.append("Script exception: ");
		s.append(getMessage());
		if (sourceName!=null) {
			s.append(" (");
			s.append(sourceName);
			if (sourceLineNumber>=0) {
				s.append(':');
				s.append(String.valueOf(sourceLineNumber));
			}
			s.append(')');
		}
		s.append('\n');
		
		StackTraceElement[] trace=getStackTrace();
		boolean hitScript=false;
		for (StackTraceElement te: trace) {
			String className=te.getClassName();
			if (className.startsWith("org.mozilla.javascript.gen.") && te.getLineNumber()>=0) {
				// JavaScript element
				hitScript=true;
				s.append('\t');
				s.append("at server-side JavaScript ");
				String fileName=te.getFileName();
				if (fileName==null) {
					s.append("(Unknown Source)");
				} else {
					s.append(fileName);
					s.append(":");
					s.append(String.valueOf(te.getLineNumber()));
				}
				s.append('\n');
			//} else if (!hitScript || (className.startsWith("net.rcode.assetserver") && !CLASS_NOSTACK_PATTERN.matcher(className).find())) {
			} else if (!hitScript && !CLASS_NOSTACK_PATTERN.matcher(className).find()) {
				s.append('\t');
				s.append("at ");
				s.append(className);
				s.append("(");
				String fileName=te.getFileName();
				if (fileName!=null) {
					s.append(fileName);
					s.append(":");
					s.append(String.valueOf(te.getLineNumber()));
				} else {
					s.append("Unknown Source");
				}
				s.append(")");
				s.append('\n');
			}
		}
	}
	
	/**
	 * Takes an exception which may or may not be a Rhino based exception and
	 * translates it to a GenericScriptException which provides a more approachable
	 * view into what happened.
	 * @param t
	 * @return new GenericScriptException
	 */
	public static Exception translateException(Exception e) {
		if (e instanceof JavaScriptException)
			return translateJavaScriptException((JavaScriptException)e);
		else if (e instanceof RhinoException)
			return translateRhinoException((RhinoException)e);
		else if (e instanceof GenericScriptException)
			return e;
		else if (e instanceof InvocationTargetException) {
			InvocationTargetException ex=(InvocationTargetException) e;
			Throwable cause=ex.getCause();
			if (cause!=null && cause instanceof Exception)
				return (Exception)cause;
			else
				return e;
		}
		else
			return e;
	}

	private static Exception translateRhinoException(RhinoException e) {
		Throwable source=e;
		String message=e.details();
		if (e instanceof WrappedException) {
			source=((WrappedException)e).getWrappedException();
			message=source.getMessage();
		}
		
		GenericScriptException gse=new GenericScriptException(e.sourceName(), e.lineNumber(), message);
		gse.initCause(source);
		gse.setStackTrace(source.getStackTrace());
		
		return gse;
	}

	private static Exception translateJavaScriptException(JavaScriptException e) {
		Object value=e.getValue();
		String message;
		if (value==null) message=e.details();
		else message=value.toString();
		
		GenericScriptException gse=new GenericScriptException(e.sourceName(), e.lineNumber(), message);
		gse.initCause(e);
		gse.setStackTrace(e.getStackTrace());
		return gse;
	}

}
