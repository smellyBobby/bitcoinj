package com.google.bitcoin.core.experimental;

import static com.google.bitcoin.core.Utils.*;
import static com.google.bitcoin.core.experimental.Utils.*;

import java.math.BigInteger;

/**
 * Base class of methods used by both QueryDisk and StoredBlocksInByteArray.
 * 
 * @author Micheal Swiggs
 *
 */
public class StoredBlocksMethods {
	public static int BLOCK_SIZE = 92;
	
	private int offset;
	
	protected void resetOffset(){offset=0;}
	
	protected BigInteger bigIntegerFromByteArray(byte[] src) {
		byte[] buf = new byte[8];
		System.arraycopy(src, offset, buf, 0, 8);
		offset+=8;
		return new BigInteger(buf);
	}

	protected int intFromByteArray(byte[] src) {
		int result = byteArrayBEToInt(src,offset);
		offset+=4;
		return result;
	}

	

	protected void intToByteArray(int n,byte[] dest){
		uint32ToByteArrayLE(n,
				dest,offset);
		offset+=4;
	}
	
	protected void hashToByteArray(byte[] hash,byte[] dest){
		System.arraycopy(hash,0,dest,offset,32);
		offset+=32;
	}
	
	protected byte[] getHashFromByteArray(byte[] source){
		byte[] result = new byte[32];
		System.arraycopy(source, offset, result, 0, 32);
		offset+=32;
		return result;
	}
	protected void chainWorkToByteArray(BigInteger dec,byte[] dest){
		byte[] result = dec.toByteArray();
		if(result.length>8)
			throw new RuntimeException("This cannot happen serious bug");
		System.arraycopy(result, 0, dest, offset, result.length);
		offset+=8;
	}
}
