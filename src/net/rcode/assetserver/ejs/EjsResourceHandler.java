package net.rcode.assetserver.ejs;

import java.io.File;

import net.rcode.assetserver.core.AssetLocator;
import net.rcode.assetserver.core.AssetPath;
import net.rcode.assetserver.core.ResourceHandler;
import net.rcode.assetserver.core.ResourceMount;

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
public class EjsResourceHandler implements ResourceHandler {

	@Override
	public AssetLocator accessResource(ResourceMount owner,
			AssetPath assetPath, File physicalResource) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
