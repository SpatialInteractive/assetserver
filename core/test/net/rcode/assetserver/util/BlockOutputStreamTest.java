package net.rcode.assetserver.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.*;

import org.junit.Test;

public class BlockOutputStreamTest {

	/**
	 * Create a new buffer of the given size where each entry is a number
	 * between 0...9 where its value is the mod 10 of its position.
	 * @param size
	 * @return byte array
	 */
	private byte[] createBuffer(int size) {
		byte[] b=new byte[size];
		for (int i=0; i<size; i++) {
			b[i]=(byte) (i%10);
		}
		return b;
	}
	
	@Test
	public void testBlockIo() throws IOException {
		byte[] src=createBuffer(96000);	// Bigger than default block size
		ByteArrayInputStream in=new ByteArrayInputStream(src);
		BlockOutputStream blockOut=new BlockOutputStream();
		byte[] buffer=new byte[8192];
		for (;;) {
			int r=in.read(buffer);
			if (r<0) break;
			blockOut.write(buffer, 0, r);
		}
		
		// Now read back
		InputStream blockIn=blockOut.openInput();
		ByteArrayOutputStream verifyOut=new ByteArrayOutputStream();
		for (;;) {
			int r=blockIn.read(buffer);
			if (r<0) break;
			verifyOut.write(buffer, 0, r);
		}
		
		// Verify
		byte[] actual=verifyOut.toByteArray();
		assertEquals("reported length", src.length, (int)blockOut.length());
		assertEquals("actual buffer length", src.length, actual.length);
		assertTrue("buffers equal", Arrays.equals(src, actual));
	}
	
	@Test
	public void testByteIo() throws IOException {
		byte[] src=createBuffer(96000);	// Bigger than default block size
		ByteArrayInputStream in=new ByteArrayInputStream(src);
		BlockOutputStream blockOut=new BlockOutputStream();
		for (;;) {
			int r=in.read();
			if (r<0) break;
			blockOut.write(r);
		}
		
		// Now read back
		InputStream blockIn=blockOut.openInput();
		ByteArrayOutputStream verifyOut=new ByteArrayOutputStream();
		for (;;) {
			int r=blockIn.read();
			if (r<0) break;
			verifyOut.write(r);
		}
		
		// Verify
		byte[] actual=verifyOut.toByteArray();
		assertEquals("reported length", src.length, (int)blockOut.length());
		assertEquals("actual buffer length", src.length, actual.length);
		assertTrue("buffers equal", Arrays.equals(src, actual));
	}

}
