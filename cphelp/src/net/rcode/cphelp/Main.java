package net.rcode.cphelp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;


/**
 * A main class that can setup a classpath based on Manifest entries and delegate
 * to a Main class within the altered classpath.
 * 
 * @author stella
 *
 */
public class Main {
	
	public static void main(String[] args) throws Throwable {
		LoaderLookup loader=LoaderLookup.getInstance();
		Properties loaderProps=loader.getLoaderProperties();
		
		String mainClassName=loaderProps.getProperty("main");
		String mainLoaderName=loaderProps.getProperty("main.loader", "this");
		ClassLoader mainLoader=loader.lookup(mainLoaderName);
		if (mainLoader==null) {
			System.err.println("ERROR: Main loader name '" + mainLoaderName + "' from config not found");
			System.exit(1);
		}
		
		if (mainClassName==null) {
			System.err.println("ERROR: No main class name found");
			System.exit(2);
		}
		
		Class<?> mainClass=Class.forName(mainClassName, true, mainLoader);
		Thread.currentThread().setContextClassLoader(mainLoader);
		
		Method mainMethod=mainClass.getMethod("main", new Class[] { String[].class });
		try {
			mainMethod.invoke(null, new Object[] { args });
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}
}
