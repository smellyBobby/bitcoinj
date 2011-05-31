package com.bitcoin.core.test.support;

import com.google.bitcoin.blockstore.experimental.StoredBlockSerializerImpl;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.genesis.GenesisBlock;

public class Support {

	static NetworkParameters n = NetworkParameters.prodNet();
	
	public static Block newBlock(){
		return new Block(n);
	}
	
	public static StoredBlockSerializerImpl storedBlockSerializer(){
	    return new StoredBlockSerializerImpl(n);
	}
	
	public static GenesisBlock newGenesisBlock(){
	    return new GenesisBlock(n);
	}
}
