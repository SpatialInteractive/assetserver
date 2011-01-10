package net.rcode.assetserver.ejs;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;

import net.rcode.assetserver.core.AssetLocator;
import net.rcode.assetserver.core.BufferAssetLocator;
import net.rcode.assetserver.core.FilterChain;
import net.rcode.assetserver.core.RequestContext;
import net.rcode.assetserver.core.ResourceFilter;
import net.rcode.assetserver.util.BlockOutputStream;
import net.rcode.assetserver.util.IOUtil;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

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
	private EjsRuntime runtime;
	private EjsCompiler compiler;

	public EjsResourceFilter() {
		super("ejs");
		runtime=new EjsRuntime();
		runtime.loadLibraryStd();
		
		compiler=new EjsCompiler(runtime);
	}

	@Override
	public AssetLocator filter(FilterChain context, AssetLocator source)
			throws Exception {
		EjsRuntime.Instance instance=runtime.createInstance();
		
		// Generate the content
		String encoding=source.getCharacterEncoding();
		if (encoding==null) encoding=context.getServer().getDefaultTextFileEncoding();
		Reader templateIn=new InputStreamReader(IOUtil.buffer(source.openInput()), encoding);
		BlockOutputStream outBuffer=new BlockOutputStream();
		OutputStreamWriter out=new OutputStreamWriter(outBuffer, encoding);
		
		Context cx=runtime.enter();
		try {
			// Establish globals
			Scriptable scope=instance.getScope();
			
			Scriptable runtime=cx.newObject(scope);
			ScriptableObject.putProperty(scope, "runtime", runtime);
			
			ScriptableObject.putProperty(runtime, "filterChain", context);
			ScriptableObject.putProperty(runtime, "server", context.getServer());
			ScriptableObject.putProperty(runtime, "requestContext", RequestContext.getInstance());
			
			Function template=compiler.compileTemplate(scope, templateIn, context.getRootFile().toString());
			Function appendableWrite=instance.createAppendableAdapter(out);
			
			ScriptableObject.putProperty(runtime, "rawWrite", appendableWrite);
			
			template.call(cx, scope, null, new Object[] { appendableWrite });
			out.flush();
		} finally {
			runtime.exit();
		}
		
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
