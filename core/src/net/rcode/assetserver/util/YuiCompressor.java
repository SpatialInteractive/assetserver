package net.rcode.assetserver.util;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.mozilla.javascript.EvaluatorException;

import net.rcode.cphelp.LoaderLookup;

/**
 * An adapter to access the YUI compressor.  Since the YUI compressor was not really
 * built for embedding, we have to load it from a separate class loader.  In addition,
 * since the YUI compressor is just an altered version of Rhino, it cannot coexist
 * in a classloader chain with Rhino.
 * 
 * @author stella
 *
 */
public class YuiCompressor {
	private static final String YUI_CLASSLOADER="yuicompressor";
	private static final String JSCOMPRESSOR_CLASS="com.yahoo.platform.yui.compressor.JavaScriptCompressor";
	private static final String CSSCOMPRESSOR_CLASS="com.yahoo.platform.yui.compressor.CssCompressor";
	private static final String ERRORREPORTER_CLASS="org.mozilla.javascript.ErrorReporter";
	private static final String EVALUATOREXCEPTION_CLASS="org.mozilla.javascript.EvaluatorException";
	
	private ClassLoader yuiClassLoader;
	private Class<?> jsCompressorClass;
	private Constructor<?> jsCompressorCtor;
	private Class<?> errorReporterClass;
	private Method jsCompressMethod;
	private Class<?> evaluatorExceptionClass;
	private Constructor<?> evaluatorExceptionCtor;
	
	private Object errorReporter;
	
	private Class<?> cssCompressorClass;
	private Constructor<?> cssCompressorCtor;
	private Method cssCompressMethod;
	
	// Options
	private int lineBreakPos=-1;
	private boolean jsMunge=true;
	private boolean verbose=false;
	private boolean jsPreserveSemiColons=false;
	private boolean jsDisableOptimizations=false;
	
	public YuiCompressor() {
		try {
			yuiClassLoader=LoaderLookup.getInstance().lookup(YUI_CLASSLOADER);
			
			// Get the JS compressor
			jsCompressorClass=Class.forName(JSCOMPRESSOR_CLASS, true, yuiClassLoader);
			errorReporterClass=Class.forName(ERRORREPORTER_CLASS, true, yuiClassLoader);
			jsCompressorCtor=jsCompressorClass.getConstructor(Reader.class, errorReporterClass);
			jsCompressMethod=jsCompressorClass.getMethod("compress", 
				Writer.class,	// Out
				Integer.TYPE,	// linebreak
				Boolean.TYPE,	// munge
				Boolean.TYPE,	// verbose
				Boolean.TYPE,	// preserve semicolons
				Boolean.TYPE	// disable optimizations
				);
			
			// Make a dynamic proxy for an ErrorReporter
			errorReporter=Proxy.newProxyInstance(yuiClassLoader, new Class[] { errorReporterClass }, errorReporterHandler);
			
			// Get the EvaluatorException, which the ErrorReporter needs
			evaluatorExceptionClass=Class.forName(EVALUATOREXCEPTION_CLASS, true, yuiClassLoader);
			evaluatorExceptionCtor=evaluatorExceptionClass.getConstructor(
					String.class,	// Detail
					String.class,	// SourceName
					Integer.TYPE,	// Line Number
					String.class,	// Line Source
					Integer.TYPE	// Column Number
					);
			
			// Get the CSS compressor
			cssCompressorClass=Class.forName(CSSCOMPRESSOR_CLASS, true, yuiClassLoader);
			cssCompressorCtor=cssCompressorClass.getConstructor(Reader.class);
			cssCompressMethod=cssCompressorClass.getMethod("compress", Writer.class, Integer.TYPE);
		} catch (Exception e) {
			throw new RuntimeException("Error accessing yui compressor via reflection", e);
		}
	}
	
	/**
	 * Dynamically create an instance of ErrorReporter via an InvocationHandler.  Implements methods
	 * "error", "runtimeError", "warning".  They all take the following signature:
	 *   String message, String sourceName, int line, String lineSource, int lineOffset
	 */
	private InvocationHandler errorReporterHandler=new InvocationHandler() {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			String methodName=method.getName();
			if ("runtimeError".equals(methodName)) {
				// Evaluator exception ctor has same args as runtimeError
				return evaluatorExceptionCtor.newInstance(args);
			} else if ("warning".equals(methodName)) {
				// Do nothing
				return null;
			} else if ("error".equals(methodName)) {
				// Raise our own excpetion (from this classloader)
				throw new EvaluatorException(
						(String)args[0],
						(String)args[1],
						((Integer)args[2]).intValue(),
						(String)args[3],
						((Integer)args[4]).intValue()
						);
			}
			
			return null;
		}
	};
	
	public void compressCss(Reader in, Writer out) {
		try {
			Object compressor=cssCompressorCtor.newInstance(in);
			cssCompressMethod.invoke(compressor, out, lineBreakPos);
		} catch (Exception e) {
			throw new RuntimeException("Error compressing CSS", e);
		}
	}
	
	public void compressJs(Reader in, Writer out) {
		try {
			Object compressor=jsCompressorCtor.newInstance(in, errorReporter);
			jsCompressMethod.invoke(compressor, out, lineBreakPos, jsMunge, verbose, jsPreserveSemiColons, jsDisableOptimizations);
		} catch (InvocationTargetException e) {
			Throwable wrapped=e.getTargetException();
			if (wrapped instanceof RuntimeException)
				throw (RuntimeException)wrapped;
			else
				throw new RuntimeException("Error compressing javascript", wrapped);
		} catch (Exception e) {
			throw new RuntimeException("Error comrpessing JS", e);
		}
	}
	
	public int getLineBreakPos() {
		return lineBreakPos;
	}

	public void setLineBreakPos(int lineBreakPos) {
		this.lineBreakPos = lineBreakPos;
	}

	public boolean isJsMunge() {
		return jsMunge;
	}

	public void setJsMunge(boolean jsMunge) {
		this.jsMunge = jsMunge;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public boolean isJsPreserveSemiColons() {
		return jsPreserveSemiColons;
	}

	public void setJsPreserveSemiColons(boolean jsPreserveSemiColons) {
		this.jsPreserveSemiColons = jsPreserveSemiColons;
	}

	public boolean isJsDisableOptimizations() {
		return jsDisableOptimizations;
	}

	public void setJsDisableOptimizations(boolean jsDisableOptimizations) {
		this.jsDisableOptimizations = jsDisableOptimizations;
	}
	
}
