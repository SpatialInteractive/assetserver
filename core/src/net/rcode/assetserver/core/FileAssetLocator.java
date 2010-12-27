package net.rcode.assetserver.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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
	public InputStream getInputStream() throws IOException {
		return new FileInputStream(file);
	}

	@Override
	public long getLength() {
		return file.length();
	}

}
