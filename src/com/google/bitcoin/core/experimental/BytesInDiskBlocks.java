package com.google.bitcoin.core.experimental;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;

import com.google.bitcoin.bouncycastle.util.Arrays;
import com.google.bitcoin.core.StoredBlock;

/**
 * This class is responsible for storing and 
 * retrieving objects to-from disk.
 * 
 * Currently the implementation does not support putting/adding 
 * StoredBlocks out of order.
 * 
 * @author Micheal Swiggs
 *
 */
public class BytesInDiskBlocks {

	public static int BLOCK_SIZE = StoredBlocksMethods.BLOCK_SIZE;
	
	File queryFile;
	RandomAccessFile randAccessFile;
	byte[] initialPrevHash;
	
	StoredBlockSerializer storedBlockSerializer;
	/**
	 * Requires guice bindings for 
	 * Named("QueryDisk") File 
	 * BlockFactory 
	 */
	public BytesInDiskBlocks(
	        File queryFile,
	        StoredBlockSerializer storedBlockSerializer) throws IOException{
		this.queryFile = queryFile;
		randAccessFile = new RandomAccessFile(queryFile,"rw");
		
		this.storedBlockSerializer = storedBlockSerializer;
	}
	
	/**
	 * Sets the initial previous hash. Should be called
	 * before load is called.
	 * @param hash - The previous hash of the first block entered. 
	 * Will most likely be the previous hash of the genesis block.
	 */
	public void setInitialPrevHash(byte[] hash){
		initialPrevHash = hash;
	}
	
	/**
	 * 
	 * @param position should correlate 
	 * to the Block's Position in the block-chain.
	 * 
	 */
	StoredBlock load(int position) throws IOException{
		byte[] src = getSource(position);
		return storedBlockSerializer.deserialize(src);
		
	}
	/**
	 *  Imposes a constraint that the previousBlockHash of the block
	 *  being stored
	 * must match the hash of the previous block stored.
	 * Blocks must be entered in Block-Chain order.
	 * 
	 * @param storedBlock - StoredBlock to be stored.
	 * @param position - The position of where the StoredBlock should be stored.
	 * @throws IOException
	 * 
	 * Also throws MemoryBlocksException.
	 *
	 */
	public void put(StoredBlock storedBlock,int position)
		throws IOException{
		validateHash(storedBlock);
		
		randAccessFile.seek(position*BLOCK_SIZE);
		randAccessFile.write(
				storedBlockSerializer.serialize(storedBlock));
	}
	private void validateHash(StoredBlock storedBlock) {
		if(Arrays.areEqual(initialPrevHash, storedBlock.getPrevByteHash())){
			initialPrevHash = storedBlock.getByteHash();
			return;
		}
		throw new ByteBlockStoreException("Previous hash mismatch");
		
	}

	private byte[] getSource(int position) throws IOException{
		
		byte[] result = new byte[BLOCK_SIZE+32];
		if(position!=0){
			randAccessFile.seek((position*BLOCK_SIZE)-32);
			randAccessFile.read(result);
		}else{
			randAccessFile.seek(0);
			randAccessFile.read(result,32,BLOCK_SIZE);
			System.arraycopy(initialPrevHash, 0, result, 0, 32);
		}
		return result;
	}
	
	
}
