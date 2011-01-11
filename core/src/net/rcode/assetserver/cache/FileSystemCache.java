package net.rcode.assetserver.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement the cache for a location.
 * @author stella
 *
 */
public class FileSystemCache implements Cache {
	private static final Logger logger=LoggerFactory.getLogger(FileSystemCache.class);
	
	private File location;
	
	public FileSystemCache(File location) {
		this.location=location;
	}
	
	/* (non-Javadoc)
	 * @see net.rcode.assetserver.cache.AssetCache#lookup(net.rcode.assetserver.cache.CacheIdentity)
	 */
	@Override
	public CacheEntry lookup(CacheIdentity identity) {
		String externalName=identity.getExternalName();
		// Loop on sequence number to find match
		for (int i=1; ; i++) {
			File seqFile=new File(location, externalName + '-' + i);
			if (!seqFile.exists()) return null;	// No hit
			
			// Try to read it
			CacheEntry candidate=safeReadFrom(seqFile, externalName);
			if (candidate!=null && identity.equals(candidate.getIdentity())) {
				return candidate;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see net.rcode.assetserver.cache.AssetCache#store(net.rcode.assetserver.cache.CacheEntry)
	 */
	@Override
	public void store(CacheEntry entry) {
		CacheIdentity identity=entry.getIdentity();
		String externalName=identity.getExternalName();
		
		try {
			// Make sure the location exists
			location.mkdirs();
			
			// First write the file to a temporary name
			File tempFile=File.createTempFile("temp", ".save", location);
			FileOutputStream out=new FileOutputStream(tempFile);
			try {
				ObjectOutputStream oout=new ObjectOutputStream(new BufferedOutputStream(out));
				// When we write, we put the identity first so that we can do
				// quick checks of identity without reading the whole contents
				// We then follow with the entry (which will output a shared
				// reference for its embedded identity field)
				oout.writeObject(entry.getIdentity());
				oout.writeObject(entry);
				
				oout.flush();
				oout.close();
				out=null;
			} finally {
				if (out!=null) out.close();
			}
			
			boolean complete=false;
			try {
				// Now find the slot to put it in
				File targetFile=null;
				for (int i=1; ; i++) {
					targetFile=new File(location, externalName + '-' + i);
					if (!targetFile.exists()) {
						// This is it.  Just save to here.
						break;
					} else {
						// It already exists.  Check the identity
						if (identity.equals(readIdentityFrom(targetFile))) {
							// This is it.  Put it in.
							targetFile.delete();
							break;
						} else {
							// Not it.
							targetFile=null;
							continue;
						}
					}
				}
				
				if (targetFile!=null) {
					// Move the temporary file into place
					if (!tempFile.renameTo(targetFile)) {
						// This is ok.  Most likely a race condition with some concurrent process.
						// Just let it go.
						// Clean up our temp file though
						tempFile.delete();
					}
				} else {
					// Shouldn't happen, but clean up our mess anyway.
					tempFile.delete();
				}
				
				complete=true;
			} finally {
				if (!complete) {
					// Exception - clean up temp file
					tempFile.delete();
				}
			}
		} catch (IOException e) {
			logger.info("IOException while storing to cache (most likely race condition unless if repetitive): " + e.getMessage());
			
			// Try to delete the whole group
			deleteByPrefix(location, externalName);
		}
	}
	
	/**
	 * Reads from the file.  On any kind of exception, return null and delete
	 * all files in the directory with the given prefix
	 * @param file
	 * @param prefix
	 * @return entry or null
	 */
	private CacheEntry safeReadFrom(File file, String prefix) {
		try {
			return readFrom(file);
		} catch (IOException e) {
			logger.info("Error reading cache file " + file + ". Deleting cache group: " + e.getMessage());
			deleteByPrefix(file.getParentFile(), prefix);
			return null;
		}
	}
	
	/**
	 * Delete all files in the given directory with the given prefix.
	 * Errors are ignored (typically concurrency issues)
	 * @param parentFile
	 * @param prefix
	 */
	private void deleteByPrefix(File dir, final String prefix) {
		File[] files=dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(prefix);
			}
		});
		
		for (File file: files) {
			file.delete();
		}
	}

	/**
	 * Read the identity from the given file.  The idenity is always stored first,
	 * so this just reads the first object, not the entire contents.  It is thus
	 * marginally efficient for doing quick identity checks.
	 * @param file
	 * @return CacheIdentity
	 * @throws IOException
	 */
	private CacheIdentity readIdentityFrom(File file) throws IOException {
		InputStream in=new FileInputStream(file);
		try {
			ObjectInputStream oin=new ObjectInputStream(new BufferedInputStream(in));
			Object ret;
			try {
				// Just read the first object from the stream
				ret=oin.readObject();
			} catch (ClassNotFoundException e) {
				throw new IOException("Could not instantiate serialized class");
			}
			
			oin.close();
			in=null;

			try {
				return (CacheIdentity) ret;
			} catch (ClassCastException e) {
				throw new IOException("Entry not of expected type");
			}
		} finally {
			if (in!=null) in.close();
		}
	}
	
	/**
	 * Read an entry from the given file.
	 * @param seqFile
	 * @return entry or null
	 * @throws IOException
	 */
	private CacheEntry readFrom(File file) throws IOException {
		InputStream in=new FileInputStream(file);
		try {
			ObjectInputStream oin=new ObjectInputStream(new BufferedInputStream(in));
			Object ret;
			try {
				// Dummy read the identity object followed by the
				// actual entry
				oin.readObject();
				ret=oin.readObject();
			} catch (ClassNotFoundException e) {
				throw new IOException("Could not instantiate serialized class");
			}
			
			oin.close();
			in=null;

			try {
				return (CacheEntry) ret;
			} catch (ClassCastException e) {
				throw new IOException("Entry not of expected type");
			}
		} finally {
			if (in!=null) in.close();
		}
	}
}
