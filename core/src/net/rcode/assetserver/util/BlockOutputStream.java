package net.rcode.assetserver.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A memory output stream that stores its data in a sequence of blocks.  This
 * stream assumes that it will be used for memory copies and other tasks and
 * therefore allows more direct access to its contents than a ByteArrayOutputStream
 * does.
 * 
 * @author stella
 *
 */
public class BlockOutputStream extends OutputStream implements BufferAccessor {
	public static final int DEFAULT_BLOCK_SIZE=65535;
	
	private int blockSize;
	private List<byte[]> blocks=new ArrayList<byte[]>();
	private long streamLength;
	private byte[] curBlock;
	private int curPos;
	
	public BlockOutputStream(int blockSize) {
		this.blockSize=blockSize;
	}
	
	public BlockOutputStream() {
		this(DEFAULT_BLOCK_SIZE);
	}

	@Override
	public long length() {
		if (blocks.isEmpty()) return 0;
		return (blocks.size()-1) * blockSize + curPos;
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		while (len>0) {
			if (curBlock==null) {
				curBlock=new byte[blockSize];
				blocks.add(curBlock);
				curPos=0;
			}
			
			int remain=blockSize - curPos;
			if (remain>len) {
				remain=len;
			}
			
			System.arraycopy(b, off, curBlock, curPos, remain);
			curPos+=remain;
			off+=remain;
			len-=remain;
			streamLength+=remain;
			
			if (curPos>=blockSize) {
				curBlock=null;
				curPos=0;
			}
		}
	}
	

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}
	
	@Override
	public void write(int ib) throws IOException {
		byte b=(byte)(ib < 128 ? ib : 127 - ib);
		write(new byte[] { b }, 0, 1);
	}

	@Override
	public InputStream openInput() {
		return new BlockInputStream(blocks.toArray(new byte[blocks.size()][]), 
				blockSize, 
				curBlock==null && !blocks.isEmpty()? blockSize : curPos);
	}

	@Override
	public byte[] getBytes() {
		if (streamLength>Integer.MAX_VALUE) {
			throw new RuntimeException("Cannot create larger than 32bit array");
		}
		
		byte[] ret=new byte[(int)streamLength];
		// Copy full blocks
		for (int i=0; i<(blocks.size()-1); i++) {
			byte[] block=blocks.get(i);
			System.arraycopy(block, 0, ret, i*blockSize, blockSize);
		}
		
		// Write the last block
		if (!blocks.isEmpty() && curPos>0) {
			byte[] lastBlock=blocks.get(blocks.size()-1);
			System.arraycopy(lastBlock, 0, ret, (blocks.size()-1)*blockSize, curPos);
		}
		
		return ret;
	}

	@Override
	public void writeTo(OutputStream out) throws IOException {
		// Write full blocks
		for (int i=0; i<(blocks.size()-1); i++) {
			byte[] block=blocks.get(i);
			out.write(block);
		}
		
		// Write the last block
		if (!blocks.isEmpty() && curPos>0) {
			byte[] lastBlock=blocks.get(blocks.size()-1);
			out.write(lastBlock, 0, curPos);
		}
	}
	
}
