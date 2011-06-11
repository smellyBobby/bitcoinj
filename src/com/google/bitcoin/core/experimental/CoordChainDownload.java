package com.google.bitcoin.core.experimental;

import java.io.IOException;

import com.google.bitcoin.core.StoredBlock;


/**
 * This class co-ordinates storing StoredBlocks from the initial 
 * chain download. 
 * 
 * @author Micheal Swiggs
 *
 */
public class CoordChainDownload {
	
	
	int chainLength;
	int memoryIndexOffset;
	HashStoreForAll hashStore;
	BytesInRamBlocks memoryStoredBlocks;
	BytesInDiskBlocksWriter byteDiskStore;

	boolean memoryPrevHashSet = false;
	
	public CoordChainDownload(HashStoreForAll hashStore,
			BytesInRamBlocks memoryStoredBlocks,
			BytesInDiskBlocksWriter byteDiskStore,
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
		memoryIndexOffset = chainLength-BytesInRamBlocks.N_INITIAL_BLOCKS;
	}
	
	public void putStoredBlock(StoredBlock storedBlock){
		hashStore.put(storedBlock.getByteHash(),storedBlock.getHeight());
		if(storedBlock.getHeight()>=(chainLength-BytesInRamBlocks.N_INITIAL_BLOCKS))
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
		int nHashesInHashStore = hashStore.nRecordedHashes();
		int length = diskChainLength-nHashesInHashStore;
		
		byte[] hashBuf = new byte[32];
		byte[] hashes = byteDiskStore.getHashes(nHashesInHashStore,
				length);
		for(int i=0;i<length;i++){
			System.arraycopy(
				hashes, i*32, hashBuf, 0, 32);
			hashStore.put(hashBuf, nHashesInHashStore++);
		}
		//Part 2 -> Refering to activity diagrams. 
		byte[] firstHashInRAM = memoryStoredBlocks.firstHash();
		int firstHashPosition = hashStore.getIndexPosition(firstHashInRAM);
		int expectedFirstHashPosition = chainLength-BytesInRamBlocks.N_INITIAL_BLOCKS;
		
		if(firstHashPosition < expectedFirstHashPosition){
		    memoryStoredBlocks.shiftBlocksDown(
		            expectedFirstHashPosition-firstHashPosition
		            );
		}else if(firstHashPosition > expectedFirstHashPosition){
		    memoryStoredBlocks.shiftBlocksUp(
		            firstHashPosition-expectedFirstHashPosition  );
		    for(int i=expectedFirstHashPosition;
		            i<firstHashPosition;i++){
		        byte[] buf = byteDiskStore.getBlockBytes(i);
		        
		        memoryStoredBlocks.putBytesWithPrevHashCheck(buf,
		                i-memoryIndexOffset);
		    }
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
