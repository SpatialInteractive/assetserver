package net.rcode.assetserver.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

/**
 * Some IO utilities that Java should really have in the SDK.  All methods that read
 * an entire stream or reader close the reader when finished.
 * 
 * @author stella
 *
 */
public class IOUtil {
	private static Set<File> deleteOnExitSet=new HashSet<File>();
	private static boolean deleteHookRegistered=false;
	
	private static class DeleteFileHook extends Thread {
		@Override
		public void run() {
			synchronized (deleteOnExitSet) {
				for (File file: deleteOnExitSet) {
					file.delete();
				}
			}
		}
	}
	
	public static void deleteOnExit(File file) {
		synchronized (deleteOnExitSet) {
			if (!deleteHookRegistered) {
				deleteHookRegistered=true;
				Runtime.getRuntime().addShutdownHook(new DeleteFileHook());
			}
			deleteOnExitSet.add(file);
		}
	}
	
	public static void cancelDeleteOnExit(File file) {
		synchronized (deleteOnExitSet) {
			deleteOnExitSet.remove(file);
		}
	}
	
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

	/**
	 * Writes a file in such a way that no two processes can access it at the same time
	 * @param destionation
	 * @param source
	 * @throws IOException 
	 */
	public static void interlockedWriteFile(File destination, BufferAccessor source) throws IOException {
		File parentDir=destination.getParentFile();
		File tmpFile=File.createTempFile("new", "ren", parentDir);
		boolean success=false;
		deleteOnExit(tmpFile);
		try {
			// Copy the streams
			InputStream in=null;
			FileOutputStream out=new FileOutputStream(tmpFile);
			try {
				in=source.openInput();
				byte[] buffer=new byte[16384];
				for (;;) {
					int r=in.read(buffer);
					if (r<0) break;
					out.write(buffer, 0, r);
				}
			} finally {
				try { out.close(); } catch (IOException e) { }
				if (in!=null) {
					try {
						in.close();
					} catch (IOException e) { }
				}
			}
			
			// Delete and rename the file
			destination.delete();
			if (!tmpFile.renameTo(destination)) {
				throw new IOException("Unable to interlocked create " + destination + ".  Possible permission problem or race condition with another process.");
			}
			success=true;
		} finally {
			if (!success) tmpFile.delete();
			cancelDeleteOnExit(tmpFile);
		}
	}
}
