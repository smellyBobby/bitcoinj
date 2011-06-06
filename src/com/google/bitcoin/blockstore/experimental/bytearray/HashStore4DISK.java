package com.google.bitcoin.blockstore.experimental.bytearray;

/**
 * Responsible for storing a sample of hashes. The hashes
 * come from StoredBlocks stored via QueryDisk.
 * 
 * @author Micheal Swiggs
 *
 */
public class HashStore4DISK {

	public static int N_BYTES = 6;
	public static int INITIAL_SAMPLE_BYTE = 19;
	byte[][] hashArray;
	
	public HashStore4DISK(int nHashes){
		hashArray = new byte[nHashes][N_BYTES];
	}
	
	public void put(byte[] hash,int position){
		byte[] dest = hashArray[position];
		for(int i=0;i<N_BYTES;i++)
			dest[i] = hash[INITIAL_SAMPLE_BYTE+i];
	}
	
	public boolean possiblePositive(byte[] hash,int position){
		byte[] sample = hashArray[position];
		for(int i=0;i<N_BYTES;i++){
			if(!(sample[i]==hash[INITIAL_SAMPLE_BYTE+i]))
				return false;
		}
		return true;
	}
}
