package net.rcode.assetserver.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Some IO utilities that Java should really have in the SDK.  All methods that read
 * an entire stream or reader close the reader when finished.
 * 
 * @author stella
 *
 */
public class IOUtil {
	public static byte[] slurpBinary(InputStream input, int sizeHint) throws IOException {
		try {
			ByteArrayOutputStream out;
			if (sizeHint>0) out=new ByteArrayOutputStream(sizeHint);
			else out=new ByteArrayOutputStream();
			
			byte[] buffer=new byte[4096];
			for (;;) {
				int r=input.read(buffer);
				if (r<0) break; if (r==0) continue;
				out.write(buffer, 0, r);
			}
			
			return out.toByteArray();
		} finally {
			input.close();
		}
	}
	
	/**
	 * Read an entire file in the given encoding
	 * @param file
	 * @param encoding
	 * @return CharSequence
	 * @throws IOException
	 */
	public static CharSequence slurpFile(File file, String encoding) throws IOException {
		InputStream input=new FileInputStream(file);
		return slurpStream(input, encoding, (int)file.length());
	}
	
	/**
	 * Read an entire Reader into a mutable CharSequence
	 * @param in
	 * @param sizeHint Initial allocation size or negative
	 * @return CharSequence (StringBuilder)
	 * @throws IOException
	 */
	public static CharSequence slurpReader(Reader in, int sizeHint) throws IOException {
		try {
			if (sizeHint<0) sizeHint=4096;
			StringBuilder ret=new StringBuilder(sizeHint);
			char[] buffer=new char[4096];
			for (;;) {
				int r=in.read(buffer);
				if (r<0) break;
				ret.append(buffer, 0, r);
			}
			
			return ret;
		} finally {
			in.close();
		}
	}
	
	/**
	 * Read from a Reader with no assumed size
	 * @param in
	 * @return CharSequence (StringBuilder)
	 * @throws IOException
	 */
	public static CharSequence slurpReader(Reader in) throws IOException {
		return slurpReader(in, -1);
	}
	
	/**
	 * Read an entire stream with the given encoding and hint as to the expected number
	 * of characters
	 * @param in
	 * @param encoding
	 * @param sizeHint
	 * @return CharSequence
	 * @throws IOException
	 */
	public static CharSequence slurpStream(InputStream in, String encoding, int sizeHint) throws IOException {
		InputStreamReader reader=new InputStreamReader(in, encoding);
		return slurpReader(reader, sizeHint);
	}
	
	/**
	 * Read a resource relative to the given path.  This method translates the exceptions
	 * to RuntimeExceptions and presumes an encoding of UTF-8
	 * @param relativeTo
	 * @param name
	 * @return CharSequence
	 */
	public static CharSequence slurpResource(Class<?> relativeTo, String name) {
		InputStream input=relativeTo.getResourceAsStream(name);
		if (input==null) {
			throw new RuntimeException("Could not find resource " + name + " relative to " + relativeTo.getName());
		}
		try {
			return slurpStream(input, "UTF-8", -1);
		} catch (IOException e) {
			throw new RuntimeException("IO Error reading resource " + name + " relative to " + relativeTo.getName(), e);
		}
	}

	/**
	 * If the input stream is not already buffered, buffer it.
	 * @param inputStream
	 * @return buffered stream
	 */
	public static InputStream buffer(InputStream inputStream) {
		if (inputStream==null) return null;
		if (inputStream instanceof BufferedInputStream || inputStream instanceof ByteArrayInputStream) return inputStream;
		return new BufferedInputStream(inputStream);
	}
}
