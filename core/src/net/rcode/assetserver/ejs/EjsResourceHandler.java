package net.rcode.assetserver.ejs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.rcode.assetserver.cache.CacheDependency;
import net.rcode.assetserver.cache.CacheEntry;
import net.rcode.assetserver.cache.CacheIdentity;
import net.rcode.assetserver.cache.CachingResourceHandler;
import net.rcode.assetserver.cache.FileCacheDependency;
import net.rcode.assetserver.core.AssetPath;
import net.rcode.assetserver.core.MimeMapping;
import net.rcode.assetserver.core.ResourceMount;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;

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
public class EjsResourceHandler extends CachingResourceHandler {
	private MimeMapping mimeMapping;
	private EjsRuntime runtime;
	private EjsCompiler compiler;

	public EjsResourceHandler() {
		runtime=new EjsRuntime();
		runtime.loadLibraryStd();
		
		compiler=new EjsCompiler(runtime);
		mimeMapping=new MimeMapping();
		mimeMapping.loadDefaults();
	}
	
	@Override
	protected CacheEntry generateResource(CacheIdentity identity, ResourceMount owner,
			AssetPath assetPath, File physicalResource) throws Exception {
		EjsRuntime.Instance instance=runtime.createInstance();
		Set<File> fileDependencies=new HashSet<File>();
		fileDependencies.add(physicalResource);
		
		int sizeHint=(int) physicalResource.length();
		if (sizeHint<=0) sizeHint=8192;
		
		// Generate the content
		ByteArrayOutputStream contents=new ByteArrayOutputStream(sizeHint*2);
		OutputStreamWriter out=new OutputStreamWriter(contents, "UTF-8");
		
		Context cx=runtime.enter();
		try {
			Function template=compiler.compileTemplate(instance.getScope(), physicalResource, "UTF-8");
			Function appendableWrite=instance.createAppendableAdapter(out);
			
			template.call(cx, instance.getScope(), null, new Object[] { appendableWrite });
			out.flush();
		} finally {
			runtime.exit();
		}
		
		// Construct the CacheEntry and return
		List<CacheDependency> dependencies=new ArrayList<CacheDependency>(fileDependencies.size());
		for (File fileDependency: fileDependencies) {
			dependencies.add(new FileCacheDependency(fileDependency));
		}
		CacheEntry entry=new CacheEntry(identity, 
				dependencies.toArray(new CacheDependency[dependencies.size()]), 
				mimeMapping.lookup(physicalResource.getName()), 
				"UTF-8", contents.toByteArray());
		return entry;
	}

}
