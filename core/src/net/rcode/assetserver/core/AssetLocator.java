package net.rcode.assetserver.core;

import java.io.IOException;
import java.io.InputStream;

/**
 * The result of resolving a path.  Used to access an asset.
 * @author stella
 *
 */
public interface AssetLocator {
	/**
	 * @return true if the locator should be cached (ie. it is expensive to regenerate)
	 */
	public boolean shouldCache();
	
	/**
	 * @return The asset's content type or null if unknown
	 */
	public String getContentType();
	
	/**
	 * @return The asset's character encoding or null if not applicable
	 */
	public String getCharacterEncoding();
	
	/**
	 * @return An HTTP compliant ETag value for the resource (or null)
	 */
	public String getETag();
	
	/**
	 * Get an input stream to the content.  The caller must close the stream.
	 * @return a freshly opened InputStream
	 * @throws IOException
	 */
	public InputStream getInputStream() throws IOException;
	
	/**
	 * @return The length of the data that will be written to out or -1 if unknown.  This should be
	 * considered a hint for buffer sizing purposes
	 */
	public long getLength();
	
}
