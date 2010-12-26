package net.rcode.assetserver.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CountingOutputStream extends FilterOutputStream {
	public long size;
	
	public CountingOutputStream(OutputStream out) {
		super(out);
	}

	@Override
	public void write(byte[] b) throws IOException {
		out.write(b);
		size+=b.length;
	}
	
	@Override
	public void write(int b) throws IOException {
		out.write(b);
		size+=1;
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
		size+=len;
	}
}
