package net.rcode.assetserver.addon;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Special classloader that supports loading from an addon bundle.
 * @author stella
 *
 */
public class AddonClassLoader extends ClassLoader {
	private URL addonUrl;
	private String[] prefixes;

	public AddonClassLoader(ClassLoader parent, URL addonUrl, String... prefixes) {
		super(parent);
		this.addonUrl=addonUrl;
		this.prefixes=prefixes.clone();
		for (int i=0; i<this.prefixes.length; i++) {
			String prefix=this.prefixes[i];
			if (prefix.startsWith("/")) prefix=prefix.substring(1);
			if (!prefix.endsWith("/")) prefix=prefix+'/';
			this.prefixes[i]=prefix;
		}
	}
	
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		String classFile=name.replace('.', '/') + ".class";
		for (String prefix: prefixes) {
			byte[] contents;
			try {
				URL url=new URL(addonUrl, prefix + classFile);
				contents=slurpUrl(url);
			} catch (IOException e) {
				// Skip.  Does not exist.
				continue;
			}
			return defineClass(name, contents, 0, contents.length);
		}
		
		return super.findClass(name);
	}

	@Override
	protected URL findResource(String name) {
		for (String prefix: prefixes) {
			try {
				URL url=new URL(addonUrl, prefix + name);
				if (urlExists(url)) return url;
			} catch (IOException e) {
				// skip
			}
		}
		return null;
	}
	
	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		List<URL> allResources=new ArrayList<URL>();
		for (String prefix: prefixes) {
			try {
				URL url=new URL(addonUrl, prefix + name);
				if (urlExists(url)) allResources.add(url);
			} catch (IOException e) {
				// skip
			}
		}
		
		return Collections.enumeration(allResources);
	}
	
	private boolean urlExists(URL url) throws IOException {
		// Sucks to do it this way, but I don't know a better way
		InputStream in=url.openStream();
		in.close();
		return true;
	}
	
	private byte[] slurpUrl(URL url) throws IOException {
		InputStream in=url.openStream();
		ByteArrayOutputStream out=new ByteArrayOutputStream(16384);
		try {
			byte[] buffer=new byte[4096];
			for (;;) {
				int r=in.read(buffer);
				if (r<0) break;
				out.write(buffer, 0, r);
			}
			out.flush();
			return out.toByteArray();
		} finally {
			in.close();
		}
	}
}
