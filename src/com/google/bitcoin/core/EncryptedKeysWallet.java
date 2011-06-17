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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
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
 * Should consider implementing method serializeEncryptedKeysOnly,
 * that is optimized at writing the keys to disk only and not transactions.
 *
 * Should consider changing the visibilty of keychain, so 
 */
public class EncryptedKeysWallet extends Wallet{
	static final int HASH_ITERATIONS = 1000;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7832747169923745467L;
	
	File file;
	private byte[] keyHash;
	private byte[] keySalt;
	
	transient private PaddedBufferedBlockCipher cipher;

	private transient byte[] encryptedKeysBuffer;
	/**
	 * File f is needed, because when-ever a key is added it must also be stored in 
	 * the file incase, the program collapses. Therefore override Wallet.addKey.
	 * 
	 * @param params
	 * @param key
	 */
	private EncryptedKeysWallet(NetworkParameters params,byte[] key,File f) {
		super(params);
		initEncryption(key);
	}
	
	private void initEncryption(byte[] key){
		cipher = new PaddedBufferedBlockCipher(
                new CBCBlockCipher(
                new BlowfishEngine() ) );
	}
	
	private void keyDerivation(byte[] key){
		byte[] salt = generateSalt();
		
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
            System.arraycopy(result, 0, tmp, 0, olen );
            result = tmp;
        }
        return result;
    }
    
    private synchronized byte[] encrypt( byte[] data ,
    		KeyParameter cipherKey)
    throws CryptoException {
        if(data==null||data.length==0){
            return new byte[0];
        }
        
        cipher.init(true,cipherKey);
        return callCipher(data);
    }
    
    
    //Note this method will cause encrypted keys to the end
    //of the key-chain.
	private void writeObject(ObjectOutputStream outputStream) throws IOException, CryptoException {
		
		List<EncryptedECKey> encryptedKeys = new ArrayList<EncryptedECKey>();
		
		//Move encrypted keys into a separate list.
		for(ECKey ecKey:keychain){
			if(ecKey.getClass().equals(EncryptedECKey.class))
				encryptedKeys.add((EncryptedECKey) ecKey);
			keychain.remove(ecKey);
		}
		//Now that keys have encrypted keys have been
		//removed, it is safe to serialize remaining keys
		//to disk.
		outputStream.defaultWriteObject();
		
		//Write to the ObjectStream if there is any
		//encrypted keys, this is needed for correct
		//deserialisation.
		//WARNING: This could become a potiental bug
		//if a malicious agent corrupts this number
		//when serialized to disk, then this will break
		//deserialization.
		int hasEncryptedKeys = 0;
		if(encryptedKeys.size()>0)hasEncryptedKeys=1;
		
		outputStream.writeByte(hasEncryptedKeys);
		
		//Encrypt the list containing encrypted keys, 
		//then serialize(store to disk).
		if(hasEncryptedKeys==1) outputStream.writeObject(encryptKeys(encryptedKeys));
		
		
		//Put encrypted keys back into the key-chain.
		keychain.addAll(encryptedKeys);
		
	}
	
	//Not sure that storing a byteArray will be retrieved correctly
	//during deserialization. If not wrap the bytes in an object, 
	//then serialize.
	private byte[] encryptKeys(List<EncryptedECKey> encryptedKeys,KeyParameter cipherKey) throws IOException, CryptoException{
		
		ByteObjectOutputStream encryptedKeysBuffer = newByteObjectOutputStream();
		encryptedKeysBuffer.writeObject(encryptedKeys);
		return encrypt(encryptedKeysBuffer.toByteArray(),cipherKey);
	}
	

	/**
	 * This will be called first during deserialization. After this 
	 * is invoked, decryptedKeys should be called.
	 * @param in
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(ObjectInputStream in) throws IOException,ClassNotFoundException{
		in.defaultReadObject();
		try {
			encryptedKeysBuffer = null;
			int hasEncryptedKeys = (int)(in.readByte() & 0xFF);
			if(hasEncryptedKeys==1){
				encryptedKeysBuffer = (byte[])in.readObject();
			}
		} catch (ClassNotFoundException e) {
	    	throw new RuntimeException(e);
	    } finally {
	    	if (in!=null) in.close();
	    }
	}
	
	/**
	 * Returns if there are any keys requiring 
	 * decryption. Should be called after deserialization. 
	 * @return
	 */
	public boolean keyDecryptionNeeded(){
		return encryptedKeysBuffer != null;
	}
	
	/**
	 * Make sure that setCipherKey(..) has been called before
	 * invoking this.
	 * 
	 * Currently there will be only one password for the whole 
	 * wallet, maybe in the future, it will be possible to have
	 * passwords assigned to individual keys.
	 * @throws CryptoException
	 * @throws IOException
	 */
	public synchronized void decryptKeys(KeyParameter cipherKey) throws CryptoException, IOException {
    	if(encryptedKeysBuffer==null)return;
		if(cipherKey==null) throw new EncryptedWalletException("Cipher Key(Password) must be set before calling decryptKeys()");
		cipher.init(false,cipherKey);
        byte[] decryptedKeys = callCipher(encryptedKeysBuffer);
        deserializeKeys(decryptedKeys);
        
    }
	
	/**
	 * This deserializes encrypted keys and adds them to the 
	 * key-chain.
	 * 
	 * @param decryptedKeys
	 * @throws IOException
	 */
	private void deserializeKeys( byte[] decryptedKeys ) throws IOException {
		ByteObjectInputStream keyInputBuffer = newByteObjectInputStream(decryptedKeys);
		ArrayList<EncryptedECKey> keys = null;
		try{
			keys = (ArrayList<EncryptedECKey>) keyInputBuffer.readObject();
			this.keychain.addAll(keys);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} finally {
			if( keyInputBuffer!= null) keyInputBuffer.close();
		}
	}
	
	/**
	 * This will serialize the wallet after the key is added to the key-chain.
	 * 
	 * @param key - The key to be added to the key-chain.
	 * @param serialize - Specifies if the wallet should be serialized.
	 * @throws IOException 
	 */
	public synchronized void addKey(ECKey key,boolean serialize) throws IOException{
		addKey(key);
		if(!serialize)return;
		saveToFile(file);
	}
	
	public void addKey(EncryptedECKey key){
		throw new RuntimeException("Check cipherKey is set");
	}
	/**
	 * This will create a key and will serialize the wallet to disk according
	 * to the serialize parameter.
	 * 
	 * @param serialize - Specifies if the wallet should be serialized.
	 * @return - A new un-encrypted ECKey instance.
	 * @throws IOException
	 */
	public synchronized ECKey createKey(boolean serialize) throws IOException{
		ECKey key = createKey();
		saveToFile(this.file);
		return key;
	}
	
	/**
	 * This will create a new EncryptedECKey and add it to the key-chain.
	 * 
	 * @param serialize - Specifies if the wallet should be serialized after the 
	 * 	key is added to the key-chain.
	 * @return 
	 * @throws IOException
	 */
	public synchronized EncryptedECKey createEncryptedKey(boolean serialize,KeyParameter cipherKey) throws IOException {
		if(cipherKey==null)throw new EncryptedWalletException("Cipher Key(Password) must be set before calling createEncryptedKey()");
		EncryptedECKey key = new EncryptedECKey();
		addKey(key,serialize);
		return key;
	}
	/**
	 * Becareful, once these are removed the wallet will not be able to process 
	 * new transactions, maybe??? . 
	 * 
	 * This method can be used to remove encrypted keys from the key-chain
	 * thereby reducing the risk of a malicious agent deducing the private
	 * keys from a heap dump. Maybe??
	 * 
	 * This method is intended to be used in tandem with decryptKeys(), which is 
	 * responsible for restoring encrypted keys.
	 * 
	 * However for the sake of completeness, then cipherKey should also be
	 * removed and only stored when either calling decryptKeys() or calling
	 * createEncryptedKey(serialize=true).
	 */
	public synchronized void removeEncryptedKeys(){
		
		for(ECKey key:keychain){
			if(key.getClass().equals(EncryptedECKey.class))
				keychain.remove(key);
		}
	}
	public synchronized void saveToFile(File f) throws IOException{
		saveToFileStream(new FileOutputStream(f));
	}
	
	public synchronized void saveToFileStream(FileOutputStream f) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(f);
		oos.writeObject(this);
		oos.close();
	}
	public static EncryptedKeysWallet loadFromFile(File f,byte[] cipherKey) throws IOException, CryptoException{
		return loadFromFileStream(new FileInputStream(f),cipherKey);
	}
	
	public static EncryptedKeysWallet loadFromFileStream(FileInputStream f, byte[] cipherKey) throws IOException, CryptoException{
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
		wallet.initEncryption(cipherKey);
		wallet.decryptKeys();
		return wallet;
	}
	
	private byte[] generateSalt() throws NoSuchAlgorithmException {
	    byte salt[] = new byte[8];
	    SecureRandom saltGen = new SecureRandom();
	    saltGen.nextBytes(salt);
	    return salt;
	}
	
	static ByteObjectOutputStream newByteObjectOutputStream() throws IOException{
		return new ByteObjectOutputStream(new ByteArrayOutputStream());
	}
	
	static ByteObjectInputStream newByteObjectInputStream(byte[] buf) throws IOException{
		return new ByteObjectInputStream(new ByteArrayInputStream(buf));
	}
	
	static class ByteObjectOutputStream extends ObjectOutputStream{
		
		public ByteObjectOutputStream(ByteArrayOutputStream out) throws IOException {
			super(out);
			boas = out;
		}
		ByteArrayOutputStream boas;
		
		public byte[] toByteArray(){
			return boas.toByteArray();
		}
	}
	
	static class ByteObjectInputStream extends ObjectInputStream{
		ByteArrayInputStream bais;
		public ByteObjectInputStream(ByteArrayInputStream in) throws IOException {
			super(in);
			bais = in;
		}
		
	}

	static public void main(String args[]) throws Exception{
		byte[] password;
		byte[] salt;
		Cipher cipher = Cipher.getInstance("");
	}
	public static class EncryptedWalletException 
		extends RuntimeException{

		/**
		 * 
		 */
		private static final long serialVersionUID = 6906379740188103603L;
		
		public EncryptedWalletException(String ob){
			super(ob);
		}
	}
	
}
