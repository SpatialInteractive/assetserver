package net.rcode.assetserver.ejs;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.concurrent.Callable;

import net.rcode.assetserver.core.AssetLocator;
import net.rcode.assetserver.core.BufferAssetLocator;
import net.rcode.assetserver.core.FilterChain;
import net.rcode.assetserver.core.RequestContext;
import net.rcode.assetserver.core.ResourceFilter;
import net.rcode.assetserver.util.BlockOutputStream;
import net.rcode.assetserver.util.GenericScriptException;
import net.rcode.assetserver.util.IOUtil;
import net.rcode.assetserver.util.RhinoUtil;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement the JavaScript Embedded preprocessor against text-based resources.
 * <p>
 * The JavaScript Embedded preprocessor is a server-side JavaScript binding
 * that is used to preprocess text based content.  Syntactically, it has been
 * structured to work well for generating JavaScript, CSS and HTML content, but
 * it is not really limited to this.
 * 
 * @author stella
 *
 */
public class EjsResourceFilter extends ResourceFilter {
	private static final Logger logger=LoggerFactory.getLogger("ejs");
	
	public EjsResourceFilter() {
		super("ejs");
	}

	@Override
	public AssetLocator filter(final FilterChain context, final AssetLocator source)
			throws Exception {
		final EjsRuntime ejsRuntime=context.getServer().getJavascriptRuntime();
		final Context cx=ejsRuntime.enter();
		try {
			return ejsRuntime.doNestedTopCall(cx, new Callable<AssetLocator>() {
				@Override
				public AssetLocator call() throws Exception {
					return filterInTopCall(ejsRuntime, cx, context, source);
				}
			});
		} catch (Exception e) {
			throw GenericScriptException.translateException(e);
		} finally {
			Context.exit();
		}
	}
	
	private AssetLocator filterInTopCall(EjsRuntime ejsRuntime, Context cx, FilterChain context, AssetLocator source) throws Exception {
		EjsCompiler compiler=new EjsCompiler(ejsRuntime);
		EjsRuntime.Instance instance=ejsRuntime.createInstance();
		
		// Generate the content
		String encoding=source.getCharacterEncoding();
		if (encoding==null) encoding=context.getServer().getDefaultTextFileEncoding();
		Reader templateIn=new InputStreamReader(IOUtil.buffer(source.openInput()), encoding);
		BlockOutputStream outBuffer=new BlockOutputStream();
		OutputStreamWriter out=new OutputStreamWriter(outBuffer, encoding);
		
		//logger.info("source: " + source + ", context=" + context.getAssetPath());
		
		// Establish globals
		Scriptable scope=instance.getScope();
		
		ScriptableObject.putProperty(scope, "whereami", "source: " + source + ", context=" + context.getAssetPath());
		Scriptable runtime=cx.newObject(scope);
		ScriptableObject.putProperty(scope, "runtime", runtime);
		
		ScriptableObject.putProperty(runtime, "filterChain", context);
		ScriptableObject.putProperty(runtime, "server", context.getServer());
		ScriptableObject.putProperty(runtime, "requestContext", RequestContext.getInstance());
		
		Function template=compiler.compileTemplate(scope, templateIn, context.getRootFile().toString());
		
		// If it was just an identity transform, skip extra work and just return the source
		if (!compiler.wasNonIdentity()) {
			return source;
		}
		
		Function appendableWrite=instance.createAppendableAdapter(out);
		
		ScriptableObject.putProperty(runtime, "rawWrite", appendableWrite);
		ScriptableObject.putProperty(scope, "params",
				new RhinoUtil.MapScriptable(context.getAssetPath().getParameters(), scope));
		
		template.call(cx, scope, null, new Object[] { appendableWrite });
		out.flush();
		
		// Return the locator
		BufferAssetLocator ret=new BufferAssetLocator(outBuffer);
		ret.setCharacterEncoding(encoding);
		ret.setContentType(source.getContentType());
		ret.setShouldCache(true);
		
		return ret;
	}
	
	
	@Override
	public String toString() {
		return "Standard Embedded JavaScript Filter";
	}
}
