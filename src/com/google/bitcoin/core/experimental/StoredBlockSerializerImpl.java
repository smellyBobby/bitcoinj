package com.google.bitcoin.core.experimental;

import static com.google.bitcoin.core.Utils.*;

import java.math.BigInteger;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.StoredBlock;


public class StoredBlockSerializerImpl 
	extends StoredBlocksMethods
	implements StoredBlockSerializer{


	NetworkParameters networkParameters;
	
	public StoredBlockSerializerImpl(NetworkParameters n){
		networkParameters = n;
	}
	
	/**
	 * This takes a StoredBlock and returns a byte array 
	 * that is 92-bytes long.
	 */
	@Override
	public byte[] serialize(StoredBlock storedBlock) {
		resetOffset();
		byte[] bytes = new byte[BLOCK_SIZE];
		Block header = storedBlock.getHeader();
		
		intToByteArray((int) header.getVersion(),bytes);
		hashToByteArray(header.getMerkleRoot(),bytes);
		intToByteArray((int)header.getTime(),bytes);
		intToByteArray((int)header.getDifficultyTarget(),bytes);
		intToByteArray((int)header.getNonce(),bytes);
		
		intToByteArray(storedBlock.getHeight(),bytes);
		chainWorkToByteArray(storedBlock.getChainWork(),bytes);
		
		hashToByteArray(header.getHash(),bytes);
		return bytes;
	}

	/**
	 * This takes a byte array that is 124-bytes long
	 * and parses the byte array, returning a StoredBlock.
	 */
	@Override
	public StoredBlock deserialize(byte[] source) {
		resetOffset();
		
		byte[] prevBlockHash = getHashFromByteArray(source);
		int version = intFromByteArray(source);
		byte[] merkleRoot = getHashFromByteArray(source);
		int time = intFromByteArray(source);
		int difficultyTarget = intFromByteArray(source);
		int nonce = intFromByteArray(source);
		int height = intFromByteArray(source);
		BigInteger chainWork = bigIntegerFromByteArray(source);
		byte[] hash = getHashFromByteArray(source);
		
		Block header = new Block(networkParameters,prevBlockHash,version,merkleRoot,
				time,difficultyTarget,nonce,hash);
		
		StoredBlock result = new StoredBlock(header,chainWork,height);
		return result;
		
	}

}
