package net.rcode.cphelp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Special classloader that loads its resources and classes by resolving them against
 * its parent but with different path prefixes.  This allows a single jar to contain
 * many non-overlapping jars internally.  Parent class loaders must allow the loading
 * of Class binaries with a call to getResource
 * @author stella
 *
 */
public class BundledClassLoader extends ClassLoader {
	private String[] prefixes;
	
	public BundledClassLoader(String[] prefixes, ClassLoader parent) {
		super(parent);
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
			String parentName=prefix + classFile;
			byte[] contents;
			try {
				contents=slurpParentResource(parentName);
			} catch (IOException e) {
				throw new ClassNotFoundException("IO error reading class resource", e);
			}
			if (contents!=null) {
				return defineClass(name, contents, 0, contents.length);
			}
		}
		
		return super.findClass(name);
	}

	@Override
	protected URL findResource(String name) {
		ClassLoader parent=getParent();
		for (String prefix: prefixes) {
			String parentName=prefix + name;
			URL ret=parent.getResource(parentName);
			if (ret!=null) return ret;
		}
		return null;
	}
	
	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		List<URL> allResources=new ArrayList<URL>();
		ClassLoader parent=getParent();
		for (String prefix: prefixes) {
			String parentName=prefix + name;
			Enumeration<URL> resources=parent.getResources(parentName);
			URL url;
			while (resources.hasMoreElements()) {
				url=resources.nextElement();
				allResources.add(url);
			}
		}
		
		return Collections.enumeration(allResources);
	}
	
	private byte[] slurpParentResource(String parentName) throws IOException {
		InputStream in=getParent().getResourceAsStream(parentName);
		//System.err.println("Loading parent resource " + parentName);
		if (in==null) return null;
		//System.err.println("Parent resource found");
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
