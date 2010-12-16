package net.rcode.assetserver.core;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The result of resolving a path.  Used to access an asset.
 * @author stella
 *
 */
public interface AssetLocator {
	/**
	 * @return The asset's content type or null if unknown
	 */
	public String getContentType();
	
	/**
	 * @return The asset's character encoding or null if not applicable
	 */
	public String getCharacterEncoding();
	
	/**
	 * Write to the given output stream.  Does not close the stream.
	 * @param out
	 * @throws IOException
	 */
	public void writeTo(OutputStream out) throws IOException;
}
