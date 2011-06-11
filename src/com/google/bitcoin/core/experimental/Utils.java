package com.google.bitcoin.core.experimental;

import static com.google.bitcoin.core.Utils.*;

public class Utils {

	
	public static int byteArrayBEToInt(byte[] arr,int offset){
		return (arr[offset] << 24)
		+ ((arr[1+offset] & 0xFF)<<16)
		+ ((arr[2+offset] & 0xFF)<<8)
		+ ((arr[3+offset] & 0xFF)<<0);
	}
	
	public static void printHash(byte[] hash){
		println(bytesToHexString(hash));
	}
	
	public static void println(Object ob){
		System.out.println(ob);
	}
}
