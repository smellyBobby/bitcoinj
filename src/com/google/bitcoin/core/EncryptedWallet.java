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

import static com.google.bitcoin.core.Utils.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.bitcoin.bouncycastle.crypto.*;
import com.google.bitcoin.bouncycastle.crypto.digests.SHA256Digest;
import com.google.bitcoin.bouncycastle.crypto.engines.*;
import com.google.bitcoin.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import com.google.bitcoin.bouncycastle.crypto.modes.*;
import com.google.bitcoin.bouncycastle.crypto.paddings.PKCS7Padding;
import com.google.bitcoin.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import com.google.bitcoin.bouncycastle.crypto.params.*;
import com.google.bitcoin.bouncycastle.util.Strings;

/**
 * This will enhance the current wallet by encrypting specified keys. The 
 * current implementation allows individual keys to be either encrypted or 
 * not. 
 * 
 * Encryption is done with AES-256-CBC, but key-derivation is done with SHA-256 100,000 rounds,
 * instead of AES256 as is done in the c++ version. 
 * 
 * This does not implement any password-handling 'protocols'. This means when 
 * someone decrypts a wallet, there is no simple way of checking if the 
 * password matches the wallet. Also the password used during encryption
 * is expected to be valid(e.g who-ever owns the wallet entered the password
 * twice). The PasswordHash class is there to help implement custom password
 * protocols.
 *
 * Should consider implementing method serializeEncryptedKeysOnly,
 * that is optimised at writing the keys to disk only and not transactions.
 *
 * Should consider changing the visibility of keychain to protected.
 * 
 * Password Notes:
 *   - When converting password Strings to byte[] passwords make sure 
 *   that the UTF-8 CharSet is used. 
 *   
 */
public class EncryptedWallet extends Wallet{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7832747169923745467L;
	
	/**
	 * This is intended to be a class with single use 'getPass' methods,
	 * thus reducing the amount of time that password is in the heap.
	 *
	 */
	static class PassHolder{
		private byte[] passBuf;
		
		public PassHolder(byte[] pass){
			passBuf = pass.clone();
		}
		
		public byte[] getPass(){
			byte[] result = passBuf.clone();
			byte[] blank = new byte[result.length];
			System.arraycopy(blank, 0, passBuf, 0, blank.length);
			passBuf = null;
			return result;
		}
		
		public boolean isNull(){return passBuf==null;}
		
		public void nullify(){
			if(passBuf!=null)getPass();
		}
	}
	
	//Cryptographic salt, MUST be persisted.
	byte[] salt;
	
	transient CbcAesBlockCipher cipher;
	transient PassHolder keyParams;
	transient List<ECKey> encryptedKeys;
	transient byte[] encryptedKeysBuffer;
	
	/**
	 * 
	 * @param params 
	 * @param f - The file where the wallet will be serialized to.
	 */
	private EncryptedWallet(NetworkParameters params) {
		super(params);
		this.salt = CbcAesBlockCipher.generateSalt();
		this.cipher = new CbcAesBlockCipher();
	}
	
	
    private void setKeyParameters(byte[] pass){
    	if(!hasEncryptedKeys()){
    		return; //Should this be an exception?
    		//Is this over-restrictive? 
    		//The intention is that there should not be any un-necessary
    		//risk of exposing the the users details to a heap-dump.
    	}
    	
    	if(keyParams!=null)keyParams.nullify();
    	keyParams = new PassHolder(pass);
    }
    //Note this method will force encrypted keys to the end
    //of the key-chain.
	private void writeObject(ObjectOutputStream outputStream) throws IOException, CryptoException {
		assert removeEncryptedKeys().size() == 0 : "encrypted keys must be removed from keychain before calling this serialization method";
		//Now that keys have encrypted keys have been
		//removed, it is safe to serialize remaining keys
		//to disk.
		outputStream.defaultWriteObject();
		
		//Write to the ObjectStream if there is any
		//encrypted keys, this is needed for correct
		//deserialisation.
		int hasEncryptedKeys = 0;
		if(encryptedKeys.size()>0)hasEncryptedKeys=1;
		
		outputStream.writeByte(hasEncryptedKeys);
		
		//Encrypt the list containing encrypted keys, 
		//then serialize(store to disk).
		if(hasEncryptedKeys==1){
			keyParametersNotNull();
			encryptedKeysBuffer = encryptKeys(encryptedKeys,keyParams.getPass());
			outputStream.writeObject(encryptedKeysBuffer);
		}
		
	}
	
	private void keyParametersNotNull(){
		if(keyParams==null || keyParams.isNull())
			throw new EncryptedWalletException("keyParams:KeyParameters must be set before encryption/decryption.");
	}
	
	private byte[] encryptKeys(List<ECKey> encryptedKeys,byte[] pass) throws IOException, CryptoException{
		ByteObjectOutputStream encryptedKeysBuffer = newByteObjectOutputStream();
		encryptedKeysBuffer.writeObject(encryptedKeys);
		return cipher.encrypt(pass, salt, encryptedKeysBuffer.toByteArray());
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
	 * 
	 * Currently there will be only one password for the whole 
	 * wallet, maybe in the future, it will be possible to have
	 * passwords assigned to individual keys. If a CryptoException 
	 * is thrown then this means that the wrong password was used,
	 * or that the serialization/deserialization of the object is 
	 * corrupted, affecting EncryptedWallet.salt .
	 * 
	 * @throws CryptoException
	 * @throws IOException
	 */
	public synchronized void decryptKeys(byte[] pass) throws CryptoException, IOException {
    	if(encryptedKeysBuffer==null)return; //Should this be an exception?

    	byte[] decryptedKeys = cipher.decrypt(pass,salt, encryptedKeysBuffer);
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
		List<ECKey> keys = null;
		try{
			keys = (List<ECKey>) keyInputBuffer.readObject();
			for(ECKey key:keys){
				if(keychain.contains(key))continue;
				keychain.add(key);
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} finally {
			if( keyInputBuffer!= null) keyInputBuffer.close();
		}
	}
	
	
	/**
	 * This will create a key. The key will be encrypted depending on the 
	 * encrypted parameter.
	 * 
	 * @param encrypted - Specifies if the key should be encrypted.
	 * @return -
	 * @throws IOException
	 */
	public synchronized ECKey createKey(boolean encrypted) throws IOException{
		ECKey key = new ECKey(encrypted);
		addKey(key);
		return key;
	}
	
	/**
	 * 
	 * This method is intended to be used in tandem with decryptKeys(), which is 
	 * responsible for restoring encrypted keys. Whenever an encrypted key is need
	 * the caller will first decrypt all encrypted keys, use the encrypted keys and then
	 * remove all encrypted keys from the heap, leaving the only trace of encrypted keys in 
	 * encryptedKeysBuffer and the List<ECKey> that is returned. The caller should
	 * handle this appropriately to remove it from the heap.
	 * 
	 * Be careful, once these are removed the wallet will not be able to process 
	 * new transactions, maybe??? .
	 */
	public synchronized List<ECKey> removeEncryptedKeys(){
		ArrayList<ECKey> encryptedKeys = new ArrayList<ECKey>();
		for(ECKey key:keychain){
			if(key.isEncrypted()){
				encryptedKeys.add(key);
			}
		}
		keychain.removeAll(encryptedKeys);
		return encryptedKeys;
	}
	
	/**
	 * The result of this method will indicate there are 
	 * keys that could be decrypted. Call this after loading the 
	 * wallet from a file to decide if to query the wallet owner 
	 * for a password and decrypt encrypted keys.
	 * 
	 * @return - If the wallet is storing encrypted keys in a buffer.
	 */
	public boolean hasEncryptedKeys(){
		for(ECKey key:keychain){
			if(key.isEncrypted())return true;
		}
		return false;
	}
	
	/**
	 * This will serialize the wallet to the specified file. The wallet 
	 * must not contain any encrypted keys.
	 */
	public void saveToFile(File f) throws IOException{
		saveToFileStream((OutputStream)new FileOutputStream(f));
	}
	
	/**
	 * This will encrypt and serialize a wallet containing encrypted keys
	 * to the specified file. If pass is null then this will be treated
	 * as a call to saveToFile(File).
	 * 
	 * @param f - Destination wallet file.
	 * @param pass - Password used for encryption, it does not have to be
	 * the same one used for decryption. But if forgotten, the next decryption
	 * will be impossible.
	 * 
	 * @param salt - Cryptographic salt.
	 * @throws IOException
	 */
	public void saveToFile(File f, byte[] pass) throws IOException{
		saveToFileStream(new FileOutputStream(f),pass);
	}
	
	/**
	 * This will encrypt and serialize a wallet containing encrypted keys
	 * to a specified file. If pass or salt is null then this is treated
	 * as a call to saveToFileStream(FileOutputStream).
	 * 
	 * @param f - Destination wallet OutputStream.
	 * @param pass - Password used for encryption, it does not have to be the
	 * sam one used for encryption. But if forgotten, the next decryption will
	 * be impossible.
	 * 
	 * @param salt - Cryptographic salt.
	 * @throws IOException
	 */
	public void saveToFileStream(OutputStream f, byte[] pass) throws IOException{
		if(!(pass==null))setKeyParameters(pass);
		saveToFileStream(f);
	}
	
	/**
	 *  This will serialize a wallet to a specified file. This should not
	 *  be called if the wallet contains encrypted keys.
	 *  
	 *  @param f - Destination wallet OutputStream.
	 */
	public synchronized void saveToFileStream(OutputStream f) throws IOException {
		//Remove encrypted keys from key-chain.
		encryptedKeys = removeEncryptedKeys();
		ObjectOutputStream oos = new ObjectOutputStream(f);
		oos.writeObject(this);
		oos.close();
		//Put encrypted keys back into key-chain.
		keychain.addAll(encryptedKeys);
		encryptedKeys = null;
	}
	
	/**
	 * This will deserialize the wallet from the specified file, but will not 
	 * decrypt encrypted keys. The caller must check if the wallet requires furthur
	 * decryption. 
	 * 
	 * @param f - The file where the wallet is stored.
	 * @return - A deserialized wallet.
	 * @throws IOException
	 */
	public static EncryptedWallet loadFromFile(File f) throws IOException {
		return loadFromFileStream(
				(InputStream)new FileInputStream(f));
	}
	
	/**
	 * This will deserialize the wallet from the InputStream.
	 * 
	 * @param f - The InputStream containing the wallet.
	 * @return - A deserialized wallet.
	 * @throws IOException
	 */
	public static EncryptedWallet loadFromFileStream(InputStream f) throws IOException{
		ObjectInputStream ois = null;
		EncryptedWallet wallet = null;
		try{
			ois = new ObjectInputStream(f);
			wallet = (EncryptedWallet) ois.readObject();
			wallet.cipher = new CbcAesBlockCipher();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} finally {
			if ( ois != null ) ois.close();
		}
		return wallet;
	}
	
	public static EncryptedWallet newEncryptedWallet(NetworkParameters n){
		return new EncryptedWallet(n);
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
	
	//Persist the salt
	static class CbcAesBlockCipher{
		static final int ITERATIONS = 100000;
		static final int AES_KEYSIZE = 256;
		static final int IV_SIZE = 128;
		static final int SALT_SIZE = 16;
		static SecureRandom secureRandom = new SecureRandom();

		static ParametersWithIV FAKEPARAMS;
		static{
			//Hopefully no one will use this as a password..
			byte[] fakeSalt = generateSalt();
			FAKEPARAMS = generateParameters(Strings.toUTF8ByteArray("A27e822eab14e1dc23496dac0155c7b13cc419b7a5c8305f4462f8c2e55339aebb56ffe88ff80daca0ab2662576f4e4fc"),fakeSalt);
		}
		
		PaddedBufferedBlockCipher cipher;
	
		public CbcAesBlockCipher(){
			CBCBlockCipher blockCipher = new CBCBlockCipher(new AESEngine());
			cipher = new PaddedBufferedBlockCipher(
					blockCipher,new PKCS7Padding());
		}
		
		public void reset(){
			//This will corrupt the memory holding 
			//the valid key
			cipher.init(true, FAKEPARAMS);
		}
		
		/**
		 * This method will encrypt some data, and it will then 
		 * reset the cipher to use FAKEPARAMS.
		 * 
		 * @param cbcAesParams
		 * @param plaintext
		 * @return
		 * @throws DataLengthException
		 * @throws IllegalStateException
		 * @throws InvalidCipherTextException
		 */
		public synchronized byte[] encrypt(byte[] pass,byte[] salt,byte[] plainData) throws DataLengthException, IllegalStateException, InvalidCipherTextException{
			ParametersWithIV cbcAesParams = generateParameters(pass,salt);
			cipher.init(true, cbcAesParams);
			byte[] result = invokeCipher(plainData);
			reset();
			return result;
		}
		
		/**
		 * This method will decrypt some data, and it will then reset
		 * the cipher to use FAKEPARAMS.
		 * 
		 * @param cbcAesParams
		 * @param encryptedData
		 * @return
		 * @throws DataLengthException
		 * @throws IllegalStateException
		 * @throws InvalidCipherTextException
		 */
		public synchronized byte[] decrypt(byte[] pass,byte[] salt, byte[] encryptedData) throws DataLengthException, IllegalStateException, InvalidCipherTextException{
			ParametersWithIV cbcAesParams = generateParameters(pass,salt);
			cipher.init(false,cbcAesParams);
			byte[] result = invokeCipher(encryptedData);
			reset();
			return result;
		}
		
		private byte[] invokeCipher(byte[] data) throws DataLengthException, IllegalStateException, InvalidCipherTextException{
			int outSize = cipher.getOutputSize(data.length);
			byte[] outputBuf = new byte[outSize];
			int outputLength = cipher.processBytes(
					data, 0, data.length, outputBuf, 0);
			outputLength += cipher.doFinal(outputBuf, outputLength);
			
			if( outputLength < outSize ){
				byte[] temp = new byte[outputLength];
				System.arraycopy(outputBuf,0,temp,0,outputLength);
				outputBuf = temp;
			}
			return outputBuf;
		}
		
		/**
		 * 
		 * @param password - encryption based password.
		 * @param salt - cryptographic salt parameter.
		 */
		static ParametersWithIV generateParameters(byte[] password, byte[] salt){
			PKCS12ParametersGenerator pGen = 
				new PKCS12ParametersGenerator(new SHA256Digest());
			pGen.init(password, salt, ITERATIONS);
			//Specify AES-256 and use an 128-bit long IV.
			return (ParametersWithIV)
				pGen.generateDerivedParameters(AES_KEYSIZE, IV_SIZE);
		}
		
		static byte[] generateSalt(){
			byte[] salt = new byte[SALT_SIZE];
			secureRandom.nextBytes(salt);
			return salt;
		}
	}
	
	/**
	 * This should be used to control access to EncryptedWallet.
	 *
	 */
	public static class PasswordHash{
		byte[] salt;
		byte[] derivedKey;
		public PasswordHash(byte[] pass){
			salt = CbcAesBlockCipher.generateSalt();
			derivedKey = genDerivedKey(salt,pass);
		}
		
		private byte[] genDerivedKey(byte[] salt,byte[] pass){
			ParametersWithIV params = 
				CbcAesBlockCipher.generateParameters(pass,salt);
			KeyParameter kp = (KeyParameter) params.getParameters();
			return kp.getKey();
		}
		
		public boolean passwordMatch(byte[] pass){
			byte[] passHash = genDerivedKey(salt,pass);
			return Arrays.equals(passHash, derivedKey);
		}
		
		public String toString(){
			return bytesToHexString(derivedKey);
		}
	}
	
}
