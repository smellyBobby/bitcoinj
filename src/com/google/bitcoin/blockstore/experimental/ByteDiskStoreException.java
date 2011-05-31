package com.google.bitcoin.blockstore.experimental;

import java.io.IOException;

public class ByteDiskStoreException extends RuntimeException {

    /**
 * 
 */
    private static final long serialVersionUID = -1724108556083661692L;

    public ByteDiskStoreException(String string) {
        super(string);
    }

    public ByteDiskStoreException(IOException e) {
        super(e);
    }
}
