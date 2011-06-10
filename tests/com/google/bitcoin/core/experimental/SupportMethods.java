package com.google.bitcoin.core.experimental;

import java.io.File;
import java.io.IOException;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.NetworkParameters;

public class SupportMethods {

	static NetworkParameters n = NetworkParameters.prodNet();
	
	public static Block newBlock(){
		return new Block(n);
	}
	
	public static StoredBlockSerializerImpl storedBlockSerializer(){
	    return new StoredBlockSerializerImpl(n);
	}
	
	public static File queryDiskFile(){
		return new File("temp/queryDiske");
	}
	
	public static File blankQueryDiskFile(){
	    File result = queryDiskFile();
	    if(result.isDirectory()){
    	    for(File file:result.listFiles()){
    	        if(file.isDirectory())continue;
    	        file.delete();
    	    }
	    }
	    try {
            result.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
	    return result;
	}
	
}
