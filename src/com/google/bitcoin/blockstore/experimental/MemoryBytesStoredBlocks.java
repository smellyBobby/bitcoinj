package com.google.bitcoin.blockstore.experimental;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import com.google.bitcoin.core.StoredBlock;


/**
 * This class uses a byte[][] to store StoredBlocks as 
 * byte[]. 
 * 
 * @author Micheal Swiggs
 *
 */
public class MemoryBytesStoredBlocks {
	
	public static String fileName = "memory.storedblocks";
	public static int BLOCK_SIZE = StoredBlocksMethods.BLOCK_SIZE;
	public static int N_BLOCKS_STORED = 50000;
	public static int N_INITIAL_BLOCKS = 48000;
	
	byte[] storedBlocksArray;
	byte[] initialPrevHash;
	
	StoredBlockSerializer storedBlockSerializer;
	
	public MemoryBytesStoredBlocks(StoredBlockSerializer blockFactory){
		storedBlocksArray = new byte[N_BLOCKS_STORED * BLOCK_SIZE];
		initialPrevHash = new byte[32];
		
		this.storedBlockSerializer = blockFactory;
	}
	
	public void persist(){
		try{
		FileOutputStream outputStream = new FileOutputStream(fileName);
		
			outputStream.write(storedBlocksArray);
		} catch (IOException e) {
			throw new MemoryBlocksException(e);
		}
	}
	
	public void load(){
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(fileName);
			inputStream.read(storedBlocksArray);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	public void setInitialPrevHash(byte[] hash){
		initialPrevHash = hash;
	}
	protected byte[] getPreviousHash(int position) {
		if(position==0)return initialPrevHash;
		byte[] result = new byte[32];
		System.arraycopy(storedBlocksArray, 
				(position*BLOCK_SIZE)-32, result, 0, 32);
		return result;
	}
	public void putWithPrevHashCheck(StoredBlock storedBlock,int position){
		byte[] prevBlockHash = getPreviousHash(position);
		if(!Arrays.equals(
				storedBlock.getPrevByteHash(),
				prevBlockHash))
			throw new MemoryBlocksException("Previous hash mismatch");
		put(storedBlock,position);
	}
	
	public void putWithoutPrevHashCheck(StoredBlock storedBlock,int position){
		put(storedBlock,position);
	}
	
	private void put(StoredBlock storedBlock,int position){
		validatePosition(position);
		byte[] buffer = storedBlockSerializer.serialize(storedBlock);
		System.arraycopy(buffer,
				0, storedBlocksArray, position*BLOCK_SIZE, buffer.length);
	}
	
	public StoredBlock getStoredBlock(int position){
		validatePosition(position);
		byte[] src = new byte[BLOCK_SIZE+32];
		if(position==0){
			System.arraycopy(storedBlocksArray,
					(position*BLOCK_SIZE),src,32,BLOCK_SIZE);
			System.arraycopy(initialPrevHash,
					0,src,0,32);
		}else{
		System.arraycopy(storedBlocksArray, 
				(position*BLOCK_SIZE)-32, src, 0, src.length);
		}
		return storedBlockSerializer.deserialize(src);
		
	}
	
	private void validatePosition(int position){
		if(position<0){
			throw new MemoryBlocksException("Position cannot be below 0.");
		}
		
		if(position>=storedBlocksArray.length){
			throw new MemoryBlocksException("Position cannot be above "+
					(storedBlocksArray.length-1));
		}
	}
	
}
