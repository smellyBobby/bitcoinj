package com.google.bitcoin.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * - Wrap defaultWriteObject / defaultReadObject
 * - Hide private key from serialization.
 * 
 * If this object is removed, in favour of using 
 * a boolean encrypt identifier on ECKey, change 
 * EncryptedKeysWallet.writeObject to reflect this.
 *
 * Added to ECKey
 *   - protected boolean encrypted.
 *   - public boolean isEncrypted()
 *   
 * Added to Wallet
 *   - createKey() : ECKey  
 */
public class EncryptedECKey 
	extends ECKey{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8032938827346298660L;

	public EncryptedECKey(){
		super();
		this.encrypted = true;
	}
	
	/**
	 * This creates a secondary ObjectOutputStream and calls writeObject(this).
	 * It then encrypts the bytes from the secondary ObjectOutputStream and writes
	 * these encrypted bytes to the primary ObjectOutputStream.
	 * 
	 * 
	 * @param oos
	 * @throws IOException
	 */

	
	
}
