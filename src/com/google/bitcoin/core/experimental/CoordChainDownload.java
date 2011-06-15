package com.google.bitcoin.core.experimental;

import static com.google.bitcoin.core.experimental.Utils.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.bitcoin.core.NetworkParameters;
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
	
	public List<byte[]> collisionHashes = new ArrayList<byte[]>();
	
	private void collision(byte[] hash){
		println(collisionHashes.size());
		collisionHashes.add(hash.clone());
		
	}

	public void load(){
		memoryStoredBlocks.load();
		
		int diskChainLength = byteDiskStore.getChainLength();
		int nHashesInHashStore = hashStore.nRecordedHashes();
		int nHashesMissingFromHashStore = diskChainLength-nHashesInHashStore;
		
		//Part 1 -> Fill HashStoreForAll with hashes it does not have.
		byte[] hashBuf = new byte[32];
		byte[] hashes = byteDiskStore.getHashes(nHashesInHashStore,
				nHashesMissingFromHashStore);
		for(int i=0;i<nHashesMissingFromHashStore;i++){
			System.arraycopy(
				hashes, i*32, hashBuf, 0, 32);
			int _collision =
				hashStore.put(hashBuf, nHashesInHashStore++);
			 if(_collision != -1){
				if(_collision != HashStoreForAll.expectedAddresses ||false){
					byte[] blockBytes = 
						byteDiskStore.getBlockBytes(_collision);
					byte[] hashTemp = Arrays.copyOfRange(blockBytes, 
							blockBytes.length-32, blockBytes.length);
					collision(hashTemp);
				}
				collision(hashBuf);
			}
		}
		//Part 2 -> Refering to activity diagrams. 
		byte[] firstHashInRAM = memoryStoredBlocks.firstHash();
		//Need to deal with the case when no StoredBlocks have been 
		//put into memoryStoredBlocks because, they were not high
		//enough in the block-chain 
		if(Arrays.equals(firstHashInRAM, NetworkParameters.prodNet().genesisBlock.getPrevBlockHash())){
			//If this passes, then  memoryStoredBlocks does not need 
			//to be loaded.
			return;
		}
		printHash(firstHashInRAM);
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
	
	public static boolean testPrint = false;
	static void testPrint(Object ob){
		if(!testPrint)return;
		System.out.println(ob);
	}
}
