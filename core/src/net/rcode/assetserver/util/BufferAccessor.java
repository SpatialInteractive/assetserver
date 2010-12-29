package net.rcode.assetserver.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Defines common ways of getting access to a buffer so that the access pattern
 * can be best matched to the storage of the data instead of just having run
 * away sequences of blind stream copies.
 * <p>
 * If you think you want to be passing a byte[] around, you probably want to be
 * passing an implementation of this interface instead.
 * 
 * @author stella
 *
 */
public interface BufferAccessor {
	/**
	 * Opens the wrapped buffer as an InputStream.  It is generally not necessary
	 * to buffer one of these further
	 * @return InputStream
	 * @throws IOException 
	 */
	public InputStream openInput() throws IOException;
	
	/**
	 * Return a byte array of the contents.  In some situations, this may return
	 * a reference to the actual backing byte array, so it should be considered
	 * read-only.
	 * @return  byte array
	 * @throws IOException 
	 */
	public byte[] getBytes() throws IOException;
	
	/**
	 * Write the contents to the given output stream
	 * @param out
	 * @throws IOException
	 */
	public void writeTo(OutputStream out) throws IOException;
	
	/**
	 * @return The length of the data that will be written to out or -1 if unknown.  This should be
	 * considered a hint for buffer sizing purposes
	 */
	public long length();

}
