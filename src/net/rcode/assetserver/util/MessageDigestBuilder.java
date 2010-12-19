package net.rcode.assetserver.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

/**
 * Build a MessageDigest consisting of arbitrary objects
 * @author stella
 *
 */
public class MessageDigestBuilder {
	private MessageDigest digest;
	private byte[] value;
	
	public MessageDigestBuilder(String algorithm) {
		try {
			digest=MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Unknown algorithm '" + algorithm + "'", e);
		}
	}
	
	public void append(String s) {
		try {
			digest.digest(s.getBytes("UTF-16BE"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Could not get UTF-16BE character set", e);
		}
	}
	
	public byte[] getValue() {
		if (value!=null) value=digest.digest();
		return value;
	}
	
	public String getValueAsHex() {
		return Hex.encodeHexString(getValue());
	}
}
