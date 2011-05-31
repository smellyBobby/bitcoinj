package com.google.bitcoin.blockstore.experimental;

import java.io.IOException;

import com.google.bitcoin.core.StoredBlock;


/**
 * This class co-ordinates storing StoredBlocks from the initial 
 * chain download. 
 * 
 * @author Micheal Swiggs
 *
 */
public class ByteBlockStoreChainDownload {
	
	
	int chainLength;
	int memoryIndexOffset;
	HashStore hashStore;
	MemoryBytesStoredBlocks memoryStoredBlocks;
	ByteWriteDiskStore byteDiskStore;

	boolean memoryPrevHashSet = false;
	
	public ByteBlockStoreChainDownload(HashStore hashStore,
			MemoryBytesStoredBlocks memoryStoredBlocks,
			ByteWriteDiskStore byteDiskStore,
			int height){
		this.hashStore = hashStore;
		this.memoryStoredBlocks = memoryStoredBlocks;
		this.byteDiskStore = byteDiskStore;
		setChainLength(height);
	}
	/**
	 * Must be set before calling putStoredBlock.
	 * 
	 * @param chainLength - the current length of the Block chain.
	 */
	private void setChainLength(int chainLength){
		this.chainLength = chainLength;
		memoryIndexOffset = chainLength-MemoryBytesStoredBlocks.N_INITIAL_BLOCKS;
	}
	
	public void putStoredBlock(StoredBlock storedBlock){
		hashStore.put(storedBlock.getHeaderHash(),storedBlock.getHeight());
		if(storedBlock.getHeight()>=(chainLength-MemoryBytesStoredBlocks.N_INITIAL_BLOCKS))
			putInMemory(storedBlock);
		
		byteDiskStore.add(storedBlock);
		
	}
	
	public void persist(){
		byteDiskStore.persist();
		memoryStoredBlocks.persist();
		
	}
	
	public void load(){
		memoryStoredBlocks.load();
		
		int diskChainLength = byteDiskStore.getChainLength();
		int nHashes = hashStore.nRecordedHashes();
		int length = diskChainLength-nHashes;
		
		byte[] hashBuf = new byte[32];
		println(diskChainLength);
		println(nHashes);
		println(length);
		byte[] hashes = byteDiskStore.getHashes(nHashes,length);
		for(int i=0;i<length;i++){
			System.arraycopy(
				hashes, i*32, hashBuf, 0, 32);
			hashStore.put(hashBuf, nHashes++);
		}
		
		
	}
	private void putInMemory(StoredBlock block){
		if(!memoryPrevHashSet){
			memoryStoredBlocks.setInitialPrevHash(block.getPrevByteHash());
			memoryPrevHashSet = true;
		}
		memoryStoredBlocks.putWithPrevHashCheck(block,block.getHeight()-memoryIndexOffset);
	}
	
	public static void println(Object ob){
		System.out.println(ob);
	}
}
