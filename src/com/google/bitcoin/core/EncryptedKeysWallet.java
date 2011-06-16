/**
 * Copyright 2011 Micheal Swiggs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.bitcoin.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.google.bitcoin.bouncycastle.crypto.*;
import com.google.bitcoin.bouncycastle.crypto.engines.*;
import com.google.bitcoin.bouncycastle.crypto.modes.*;
import com.google.bitcoin.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import com.google.bitcoin.bouncycastle.crypto.params.*;
/**
 * This will enhance the current wallet by encrypting the keys upon 
 *
 */
public class EncryptedKeysWallet extends Wallet{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7832747169923745467L;

	private String passwordHash;
	private byte[] encryptedKeys;
	File file;
	
	transient private KeyParameter key;
	transient private PaddedBufferedBlockCipher cipher;
	/**
	 * File f is needed, because when-ever a key is added it must also be stored in 
	 * the file incase, the program collapses. Therefore override Wallet.addKey.
	 * 
	 * @param params
	 * @param key
	 */
	protected EncryptedKeysWallet(NetworkParameters params,String key,File f) {
		super(params);
		initEncryption(key.getBytes());
	}
	
	private void initEncryption(byte[] key){
		cipher = new PaddedBufferedBlockCipher(
                new CBCBlockCipher(
                new BlowfishEngine() ) );
        
        this.key = new KeyParameter( key );
	}
	
	// Private routine that does the gritty work.
    private byte[] callCipher( byte[] data )
    throws CryptoException {
        int size = cipher.getOutputSize( data.length );
        byte[] result = new byte[size];
        int olen = cipher.processBytes(data,0,
                data.length,result,0);
        olen += cipher.doFinal( result, olen );
        
        if( olen < size ){
            byte[] tmp = new byte[olen];
            System.arraycopy(
                    result, 0, tmp, 0, olen );
            result = tmp;
        }
        return result;
    }
    
    private synchronized byte[] encrypt( byte[] data )
    throws CryptoException {
        if(data==null||data.length==0){
            return new byte[0];
        }
        
        cipher.init(true,key);
        return callCipher(data);
    }
    
    
    private synchronized decryptKeys() throws CryptoException {
    	assert encryptedKeys != null : "encryptedBytes can not be null";
        cipher.init(false,key);
        byte[] decryptedKeys = callCipher(encryptedKeys);
        deserializeKeys(decryptedKeys);
    }
    
	private void writeObject(ObjectOutputStream outputStream) throws IOException, CryptoException {
		encryptKeys();
		List<ECKey> keysTemp = new ArrayList<ECKey>();
		keysTemp.addAll(keychain);
		//Clear all keys
		keychain.clear();
		assert keychain.size()==0:"keychain should not contain keys before serialization";
		outputStream.defaultWriteObject();
		
		//Put keys back
		keychain.addAll(keysTemp);
		
	}
	
	//Not sure that storing a byteArray will be retrieved correctly
	//during deserialization. If not wrap the bytes in an object, 
	//then serialize.
	private void encryptKeys() throws IOException, CryptoException{
		ArrayList<ECKey> keys = new ArrayList<ECKey>();
		keys.addAll(keychain);
		ByteArrayOutputStream keyChainByteBuffer = new ByteArrayOutputStream();
		ObjectOutputStream keysOutputStream = new ObjectOutputStream(keyChainByteBuffer);
		keysOutputStream.writeObject(keys);
		byte[] keyChainBytes = keyChainByteBuffer.toByteArray();
		encryptedKeys = encrypt(keyChainBytes);
	}
	
	private void deserializeKeys( byte[] decryptedKeys ) throws IOException {
		ByteArrayInputStream keyInputBuffer = new ByteArrayInputStream(decryptedKeys);
		ObjectInputStream keyInputStream = new ObjectInputStream(keyInputBuffer);
		ArrayList<ECKey> keys = null;
		try{
			keys = (ArrayList<ECKey>) keyInputStream.readObject();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} finally 
	}
	private void readObject(ObjectInputStream in) throws IOException,ClassNotFoundException{
		in.defaultReadObject();
		try {
			 this.encryptedKeys = (byte[])in.readObject();
		} catch (ClassNotFoundException e) {
	    	throw new RuntimeException(e);
	    } finally {
	    	if (in!=null) in.close();
	    }
	}
	
	@Override
	public synchronized void addKey(ECKey key){
		//assert !keychain.contains(key);
		throw new UnsupportedOperationException();
	}
	
	public synchronized ECKey createKey() throws IOException{
		ECKey key = new ECKey();
		assert !keychain.contains(key);
		keychain.add(key);
		saveToFile(this.file);
		return key;
	}
	
	public synchronized void saveToFile(File f) throws IOException{
		saveToFileStream(new FileOutputStream(f));
	}
	
	public synchronized void saveToFileStream(FileOutputStream f) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(f);
		oos.writeObject(this);
		oos.close();
	}
	public static EncryptedKeysWallet loadFromFile(File f,String key) throws IOException{
		return loadFromFileStream(new FileInputStream(f),key);
	}
	
	public static EncryptedKeysWallet loadFromFileStream(FileInputStream f, String key) throws IOException{
		ObjectInputStream ois = null;
		EncryptedKeysWallet wallet = null;
		try{
			ois = new ObjectInputStream(f);
			wallet = (EncryptedKeysWallet) ois.readObject();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} finally {
			if ( ois != null ) ois.close();
		}
		wallet.initEncryption(key.getBytes());
		wallet.decryptKeys();
		return wallet;
	}
	
	
}
