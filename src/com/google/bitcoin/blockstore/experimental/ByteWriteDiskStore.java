package com.google.bitcoin.blockstore.experimental;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.genesis.GenesisBlock;



/**
 * This class is intended to be used during the initial
 * block-chain download. It is responsible for writing
 * StoredBlocks to disk, in a format that is recognised
 * by {@link QueryDisk}.
 * @author Micheal Swiggs
 *
 */
public class ByteWriteDiskStore {
    static Logger logger = Logger.getLogger("ByteWriteDiskStore");
	public static int BUFFER_SIZE = 92200;
	public static int BLOCK_SIZE = StoredBlocksMethods.BLOCK_SIZE;
	
	Block previousBlock;
	StoredBlockSerializer storedBlockSerializer;
	File storageFile;
	FileOutputStream outputStream;
	byte[] fileBuffer = new byte[BUFFER_SIZE];
	int bufferIndex = 0;
	
	public ByteWriteDiskStore(GenesisBlock genesisBlock,
			StoredBlockSerializer storedBlockSerializer,
			File file) throws IOException{
		this.previousBlock = genesisBlock;
		this.storedBlockSerializer = storedBlockSerializer;
		this.storageFile = file;
		outputStream = new FileOutputStream(file);
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
			outputStream.write(fileBuffer,0,bufferIndex);
			outputStream.flush();
			bufferIndex = 0;
		} catch (IOException e) {
			throw new ByteDiskStoreException(e);
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
	
	private void checkByteArrayCapacity() {
		if(bufferIndex<(BUFFER_SIZE-200))return;
		try {
			outputStream.write(fileBuffer,0,bufferIndex);
			outputStream.flush();
			bufferIndex=0;
		} catch (IOException e) {
			throw new ByteDiskStoreException(e);
		}
	}
	
	private void validateBlock(StoredBlock block){
		if(!block.getPrevShaHash().equals(previousBlock.getShaHash())){
		   throw new ByteDiskStoreException("Previous hash mismatch");
		}
		previousBlock = block.getHeader();
	}
	
	public static void println(Object ob){
		System.out.println(ob);
	}
	
	
	

}
