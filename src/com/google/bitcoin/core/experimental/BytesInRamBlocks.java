package com.google.bitcoin.core.experimental;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import com.google.bitcoin.core.StoredBlock;
import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;


/**
 * This class uses a byte[][] to store StoredBlocks as 
 * byte[]. 
 * 
 * @author Micheal Swiggs
 *
 */
public class BytesInRamBlocks {
	
	public static String fileName = "memory.storedblocks";
	public static int BLOCK_SIZE = StoredBlocksMethods.BLOCK_SIZE;
	//Change N_BLOCKS_STORED in 1000 (in|de)crements only 
	//otherwise algorithms will break.
	public static int N_BLOCKS_STORED = 50000;
	public static int N_INITIAL_BLOCKS = 48000;
	
	byte[] storedBlocksArray;
	byte[] initialPrevHash;
	
	StoredBlockSerializer storedBlockSerializer;
	
	public BytesInRamBlocks(StoredBlockSerializer blockFactory){
		storedBlocksArray = new byte[N_BLOCKS_STORED * BLOCK_SIZE];
		initialPrevHash = new byte[32];
		
		this.storedBlockSerializer = blockFactory;
	}
	
	public void persist(){
		try{
		FileOutputStream outputStream = new FileOutputStream(fileName);
		
			outputStream.write(storedBlocksArray);
		} catch (IOException e) {
			throw new ByteBlockStoreException(e);
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
			throw new ByteBlockStoreException("Previous hash mismatch");
		put(storedBlock,position);
	}
	
	public void putBytesWithPrevHashCheck(byte[] blockBytes,int position){
		byte[] prevBlockHashStored = getPreviousHash(position);
		byte[] prevHash = Arrays.copyOfRange(blockBytes, 0, 32);
		if(!Arrays.equals(prevBlockHashStored, prevHash))
			throw new ByteBlockStoreException("Previous hash mismatch");
		validatePosition(position);
		System.arraycopy(blockBytes, 0, storedBlocksArray, 
				position*BLOCK_SIZE, blockBytes.length);
		
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
			throw new ByteBlockStoreException("Position cannot be below 0.");
		}
		
		if(position>=storedBlocksArray.length){
			throw new ByteBlockStoreException("Position cannot be above "+
					(storedBlocksArray.length-1));
		}
	}

	/**
	 * Returns the firstHash in the byteArray
	 * 
	 * @return - 32-byte hash.
	 */
	public byte[] firstHash(){
	    byte[] hash = new byte[32];
	    System.arraycopy(
	            storedBlocksArray, 
	            BLOCK_SIZE-32, hash, 0, 32);
	    return hash;
	}
	/**
	 * Returns the last valid hash loaded. This means that a 
	 * search is done through the byteArray and the hash
	 * of last non-blank Block is returned.
	 * 
	 * @return - 32-byte hash.
	 */
    public byte[] lastHash() {
        byte[] blankArray = new byte[BLOCK_SIZE];
        byte[] buf;
        byte[] hash = new byte[32];
        //reverse search
        for(int i=N_BLOCKS_STORED;i>=0;--i){
            buf = Arrays.copyOfRange(
                    storedBlocksArray,
                    i*BLOCK_SIZE,
                    (i+1)*BLOCK_SIZE);
            if(!Arrays.equals(buf,blankArray)){
                System.arraycopy(buf, 
                    BLOCK_SIZE-32, hash, 0, 32);
                break;
            }
        }
        return hash;
    }

    /**
     * Offsets the byteArray, moving all blocks towards
     * the start by the amount defined by offset.
     * The oldest
     * blocks(blocks at the start of the byteArray)
     *  are removed.
     *  
     * @param offset - The distance blocks should move
     * towards the start.
     */
    public void shiftBlocksDown(int offset) {
        //Have to do 1000 Block shift at a time,
        //bounding to around ram-usage to 1000*BLOCK_SIZE = 92kB.
        //As System.arraycopy does not do inplace copy when
        //operating on the same object.
        
        if(offset==0)return;
        
        int index = offset;
        int copyLength = 1000*BLOCK_SIZE;
        
        while(index+copyLength<storedBlocksArray.length){
            System.arraycopy(
                    storedBlocksArray, index, 
                    storedBlocksArray, index-offset, 
                    copyLength);
            index+=copyLength;
        }
        
        System.arraycopy(
                storedBlocksArray, index, 
                storedBlocksArray, index-offset, 
                storedBlocksArray.length-index);
    }

    /**
     * Shifts the byteArray, moving all blocks towards 
     * the end by an amount defined by offset.
     * 
     * The num(offset) blocks at the end are removed
     * (The num(offset) most recent blocks).
     * 
     * @param offset - The distance blocks should move 
     * towards the end.
     */
    public void shiftBlocksUp(int offset) {
        if(offset==0)return;
        
        int index = 0;
        int copyLength = 1000*BLOCK_SIZE;
        
        while((index+offset)+copyLength<storedBlocksArray.length){
            System.arraycopy(
               storedBlocksArray, index, 
               storedBlocksArray, index+offset, 
               copyLength);
            index+=copyLength;
        }
        
        System.arraycopy(
                storedBlocksArray, index, 
                storedBlocksArray, index+offset, 
                storedBlocksArray.length-(index+offset));
        
    }

    /**
     * This takes a byte representation of 
     * @param blockBytes
     * @param i
     */
    public void putBytes(byte[] blockBytes, int i) {
        System.arraycopy(blockBytes,
                0, storedBlocksArray, i*BLOCK_SIZE, blockBytes.length);
    }
	
}
