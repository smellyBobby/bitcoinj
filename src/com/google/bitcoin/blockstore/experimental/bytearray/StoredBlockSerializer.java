package com.google.bitcoin.blockstore.experimental.bytearray;

import com.google.bitcoin.core.StoredBlock;



public interface StoredBlockSerializer {

	public byte[] serialize(StoredBlock storedBlock);
	
	public StoredBlock deserialize(byte[] source);
}
