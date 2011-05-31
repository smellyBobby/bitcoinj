package com.google.bitcoin.genesis;

import java.util.List;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.NetworkParameters;

public class GenesisBlock 
	extends Block{

	public GenesisBlock(NetworkParameters params) {
		super(params);
		addTransaction(new GenesisTransaction(params));
	}
	
	@Override 
	public GenesisBlock cloneAsHeader(){
	    getMerkleRoot();
	    
	    GenesisBlock result = new GenesisBlock(params);
	    result.setMerkleRoot(getMerkleRoot());
	    result.setTime(getTime());
	    result.setDifficultyTarget(getDifficultyTarget());
	    result.setNonce(getNonce());
	    result.setHash(getHash());
	    result.setTransactions(null);
	    String genesisHash = getHashAsString();
	    String expected = "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f";
	    if(!genesisHash.equals(expected) 
        ) throw new RuntimeException("GenesisBlock is wrong,\n"
                                            +genesisHash
                                            +"\n instead of:\n"
                                            +expected);
        return result;
	}

    

}
