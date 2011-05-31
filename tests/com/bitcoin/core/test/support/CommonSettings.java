package com.bitcoin.core.test.support;

import java.io.File;
import java.io.IOException;

public class CommonSettings {

	
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
