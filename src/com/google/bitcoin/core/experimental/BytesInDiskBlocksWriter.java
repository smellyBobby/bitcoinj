package com.google.bitcoin.core.experimental;

import static com.google.bitcoin.core.experimental.Utils.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.logging.Logger;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.StoredBlock;



/**
 * This class is intended to be used during the initial
 * block-chain download. It is responsible for writing
 * StoredBlocks to disk, in a format that is recognised
 * by {@link BytesInDiskBlocks}.
 * @author Micheal Swiggs
 *
 */
public class BytesInDiskBlocksWriter {
    static Logger logger = Logger.getLogger("ByteWriteDiskStore");
	public static int BUFFER_SIZE = 92200;
	public static int BLOCK_SIZE = StoredBlocksMethods.BLOCK_SIZE;
	
	Block previousBlock;
	StoredBlockSerializer storedBlockSerializer;
	File storageFile;
	RandomAccessFile randomAccessFile;
	byte[] initialPrevHash;
	
	byte[] fileBuffer = new byte[BUFFER_SIZE];
	int bufferIndex = 0;
	
	public BytesInDiskBlocksWriter(
	        Block genesisBlock,
			StoredBlockSerializer storedBlockSerializer,
			File file) throws IOException{
		this.previousBlock = genesisBlock;
		this.storedBlockSerializer = storedBlockSerializer;
		this.storageFile = file;
		this.initialPrevHash = genesisBlock.getHash();
		
		randomAccessFile = new RandomAccessFile(file,"rw");
	}
	/**
	 * Call this to add another block to disk. Will 
	 * throw an exception if the StoredBlock add is 
	 * not in block-chain order. I.E StoredBlocks must
	 * be added in block-chain order.
	 * 
	 * @param storedBlock the block to be stored.
	 */
	public void add(StoredBlock storedBlock) {
		appendToByteArray(storedBlock);
		checkByteArrayCapacity();
	}
	/**
	 * Called when the contents of the file buffer needs 
	 * to be written to disk.
	 */
	public void persist(){
		try {
			randomAccessFile.write(fileBuffer,0,bufferIndex);
			bufferIndex = 0;
		} catch (IOException e) {
			throw new ByteBlockStoreException(e);
		}
		
	}
	
	public int getChainLength() {
		return (int) (storageFile.length() / StoredBlockSerializerImpl.BLOCK_SIZE);
	}
	/**
	 * This is will extract from disk (length) of hashes
	 * beginning with hash specified by height in the 
	 * block-chain.
	 * 
	 * @param height - The initial hash from within the block-chain.
	 * @param length - The number of hashes to be extracted.
	 * @return  byte[] array with size corresponding to
	 * HASH_SIZE * length
	 */
	public byte[] getHashes(int height,int length){
		try {
			return _getHashes(height,length);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public byte[] _getHashes(int height, int length) 
		throws IOException{
		byte[] resultArray = new byte[length*32];
		if(length==0)return resultArray;
		RandomAccessFile stream = new RandomAccessFile(storageFile,"r");
		stream.seek(height*BLOCK_SIZE);
		byte[] fileB = new byte[BLOCK_SIZE*10000];
		int byteIndex = 0;
		int hashCount = 0;
		while(true){
			stream.read(fileB);
			byteIndex = BLOCK_SIZE-32;
			int extractCount = 10000;
			while((extractCount--)>0){
				System.arraycopy(fileB, 
						byteIndex, 
						resultArray, hashCount*32, 32);
				byteIndex+=BLOCK_SIZE;
				if(++hashCount==length)break;
			}
			if(hashCount==length)break;
		}
		return resultArray;
	}
	private void appendToByteArray(StoredBlock block){
		validateBlock(block);
		byte[] buff = storedBlockSerializer.serialize(block);
		System.arraycopy(buff, 0, fileBuffer, bufferIndex, buff.length);
		bufferIndex+=buff.length;
	}
	
	/**
	 * This will return the bytes of the block at num(height).
	 * 
	 * @param height - The height of the needed block
	 * @return - byte representation of the StoredBlock.
	 */
	public byte[] getBlockBytes(int height){
	    try {
            return _getBlockBytes(height);
        } catch (IOException e) {
            throw new ByteBlockStoreException(e);
        }
	}
	private byte[] _getBlockBytes(int height) throws IOException{
        
        byte[] result = new byte[BLOCK_SIZE+32];
        if(height!=0){
            randomAccessFile.seek((height*BLOCK_SIZE)-32);
            randomAccessFile.read(result);
        }else{
            randomAccessFile.seek(0);
            randomAccessFile.read(result,32,BLOCK_SIZE);
            System.arraycopy(initialPrevHash, 0, result, 0, 32);
        }
        return result;
    }

	private void checkByteArrayCapacity() {
		if(bufferIndex<(BUFFER_SIZE-200))return;
		try {
			randomAccessFile.write(fileBuffer,0,bufferIndex);
			bufferIndex=0;
		} catch (IOException e) {
			throw new ByteBlockStoreException(e);
		}
	}
	
	private void validateBlock(StoredBlock block){
		if(!Arrays.equals(block.getPrevByteHash(),previousBlock.getHash())){
		   printHash(block.getPrevByteHash());
		   printHash(previousBlock.getHash());
			throw new ByteBlockStoreException("Previous hash mismatch");
		}
		previousBlock = block.getHeader();
	}
	
	public static void println(Object ob){
		System.out.println(ob);
	}
	
	
	

}
