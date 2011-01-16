package net.rcode.assetserver.ejs;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import net.rcode.assetserver.ejs.EjsParser.LocationInfo;
import net.rcode.assetserver.util.IOUtil;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Compiles an EJS source file into a Rhino script for execution.
 * @author stella
 *
 */
public class EjsCompiler {
	private EjsRuntime runtime;
	
	public EjsCompiler(EjsRuntime runtime) {
		this.runtime=runtime;
	}
	
	public EjsRuntime getRuntime() {
		return runtime;
	}
	
	/**
	 * Convenience method to compile a resource
	 * @param scope
	 * @param relativeTo
	 * @param resourceName
	 * @return template function
	 */
	public Function compileTemplate(Scriptable scope, Class<?> relativeTo, String resourceName) {
		return compileTemplate(scope, IOUtil.slurpResource(relativeTo, resourceName), resourceName);
	}
	
	/**
	 * Convenience to compile from a Reader
	 * @param scope
	 * @param in
	 * @param sourceName
	 * @return
	 * @throws IOException
	 */
	public Function compileTemplate(Scriptable scope, Reader in, String sourceName) throws IOException {
		CharSequence source=IOUtil.slurpReader(in);
		return compileTemplate(scope, source, sourceName);
	}
	
	/**
	 * Convenience to compile a file.
	 * @param scope
	 * @param file
	 * @return Function
	 * @throws IOException 
	 */
	public Function compileTemplate(Scriptable scope, File file, String encoding) throws IOException {
		CharSequence source=IOUtil.slurpFile(file, encoding);
		return compileTemplate(scope, source, file.toString());
	}
	
	/**
	 * Compile the template to a Function that when invoked produces the template
	 * output.  The returned function has the following signature:
	 * <pre>
	 * 	function(write)
	 * </pre>
	 * The write argument is a function that must take a single String coercible
	 * argument and output it to its destination.
	 * 
	 * @param source
	 * @param sourceName
	 * @return Function that generates contents
	 */
	public Function compileTemplate(final Scriptable scope, CharSequence source, final String sourceName) {
		final Context cx=runtime.enter();
		try {
			final List<Object> fragments=new ArrayList<Object>(256);
			
			EjsParser.Events events=new EjsParser.Events() {
				public void handleLiteral(CharSequence text, LocationInfo location) {
					fragments.add(text.toString());
				}
				public void handleInterpolation(CharSequence script, LocationInfo location) {
					// Compile the generator function
					StringBuilder interpDefn=new StringBuilder(script.length()+50);
					interpDefn.append("function(){var expr=")
						.append(script).append(";return (expr===null||expr===undefined) ? null : String(expr);}");
					Function interpFunction=cx.compileFunction(scope, interpDefn.toString(), 
							sourceName, location.getLineStart(), null);
					fragments.add(interpFunction);
				}
				public void handleBlock(CharSequence script, LocationInfo location) {
					Script blockScript=cx.compileString(script.toString(), sourceName, location.getLineStart(), null);
					fragments.add(blockScript);
				}
			};
			
			EjsParser parser=new EjsParser(events);
			parser.parse(source);
			
			// Call the evaluator generator with the fragments to get the generator
			//return (Function) evaluatorFunction.call(cx, scope, null, new Object[] { fragments });
			return new GeneratorFunction(fragments);
		} finally {
			runtime.exit();
		}
	}
	
	private static class GeneratorFunction extends ScriptableObject implements Function {
		private List<Object> fragments;
		
		public GeneratorFunction(List<Object> fragments) {
			this.fragments=fragments;
		}
		
		@Override
		public Object call(Context cx, Scriptable scope, Scriptable thisObj,
				Object[] args) {
			// The first argument must be the writer
			Function writer=(Function) args[0];
			for (Object fragment: fragments) {
				if (fragment instanceof String) {
					writer.call(cx, scope, null, new Object[] { fragment });
				} else if (fragment instanceof Script) {
					((Script)fragment).exec(cx, scope);
				} else if (fragment instanceof Function) {
					Object interpValue=((Function)fragment).call(cx, scope, null, new Object[0]);
					if (interpValue!=null) {
						writer.call(cx, scope, null, new Object[] { interpValue });
					}
				} else {
					throw new IllegalStateException("Unreocnized type in fragments");
				}
			}
			
			return null;
		}

		@Override
		public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
			return null;
		}

		@Override
		public String getClassName() {
			return null;
		}
	}
}
