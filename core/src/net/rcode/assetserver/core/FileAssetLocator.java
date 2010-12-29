package net.rcode.assetserver.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.rcode.assetserver.util.IOUtil;

/**
 * An AssetLocator based on a physical file
 * @author stella
 *
 */
public class FileAssetLocator implements AssetLocator {
	private File file;
	private String contentType;
	private String characterEncoding;
	
	public FileAssetLocator(File file) {
		this.file=file;
		this.contentType="application/octet-string";
		this.characterEncoding=null;
	}
	
	public File getFile() {
		return file;
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}

	@Override
	public boolean shouldCache() {
		return false;
	}
	
	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public String getCharacterEncoding() {
		return characterEncoding;
	}

	@Override
	public String getETag() {
		return "F:" + file.lastModified() + ":" + file.length();
	}

	@Override
	public InputStream openInput() throws FileNotFoundException {
		return new FileInputStream(file);
	}

	@Override
	public byte[] getBytes() throws IOException {
		return IOUtil.slurpBinary(openInput(), -1);
	}

	@Override
	public void writeTo(OutputStream out) throws IOException {
		InputStream input=openInput();
		try {
			byte[] buffer=new byte[4096];
			for (;;) {
				int r=input.read(buffer);
				if (r<0) break;
				out.write(buffer, 0, r);
			}
		} finally {
			input.close();
		}
	}

	@Override
	public long length() {
		return file.length();
	}

}
