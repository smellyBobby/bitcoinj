package com.google.bitcoin.blockstore.experimental;

import java.io.IOException;

public class MemoryBlocksException extends RuntimeException{
    /**
     * 
     */
    private static final long serialVersionUID = -8838844833080912124L;

    public MemoryBlocksException(String str){
        super(str);
    }

    public MemoryBlocksException(Exception e) {
        super(e);
    }
}
