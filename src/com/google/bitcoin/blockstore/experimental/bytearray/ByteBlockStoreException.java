package com.google.bitcoin.blockstore.experimental.bytearray;

import java.io.IOException;

public class ByteBlockStoreException extends RuntimeException {

    /**
 * 
 */
    private static final long serialVersionUID = -1724108556083661692L;

    public ByteBlockStoreException(String string) {
        super(string);
    }

    public ByteBlockStoreException(IOException e) {
        super(e);
    }
}
