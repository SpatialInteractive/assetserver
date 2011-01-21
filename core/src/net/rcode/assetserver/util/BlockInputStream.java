package net.rcode.assetserver.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Create a memory InputStream over a sequence of blocks.  Instances of this class will
 * be returned from BlockOutputStream and are generally not very useful outside of that
 * context.
 * 
 * @author stella
 *
 */
public class BlockInputStream extends InputStream {
	private byte[][] blocks;
	private int lastLength;
	private int blockSize;
	
	private long position;
	
	public BlockInputStream(byte[][] blocks, int blockSize, int lastLength) {
		this.blocks=blocks;
		this.blockSize=blockSize;
		this.lastLength=lastLength;
	}
	
	@Override
	public int available() throws IOException {
		return blocks.length * blockSize;
	}
	
	@Override
	public long skip(long n) throws IOException {
		long origPosition=position, maxLength=(blocks.length-1)*blockSize + lastLength;
		position+=n;
		if (position>maxLength) position=maxLength;
		return position-origPosition;
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int blockIndex=(int)(position / blockSize);
		int blockPos=(int)(position % blockSize);
		boolean lastBlock=(blockIndex==(blocks.length-1));
		if (blockIndex>=blocks.length || (lastBlock && blockPos>=lastLength)) return -1;
		
		byte[] block=blocks[(int)blockIndex];
		int remain;
		if (lastBlock) {
			// Last block
			remain=lastLength-blockPos;
		} else {
			// Before last block
			remain=blockSize-blockPos;
		}
		
		if (remain>len) remain=len;
		System.arraycopy(block, blockPos, b, off, remain);
		
		position+=remain;
		return remain;
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}
	
	@Override
	public int read() throws IOException {
		byte[] ba=new byte[1];
		if (read(ba)<0) return -1;
		byte b=ba[0];
		return b<0 ? ((int)b) + 256 : b;
	}

}
