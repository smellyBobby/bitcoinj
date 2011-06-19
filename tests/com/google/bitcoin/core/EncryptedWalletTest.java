package com.google.bitcoin.core;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.bitcoin.bouncycastle.crypto.CryptoException;
import com.google.bitcoin.core.EncryptedWallet.CbcAesBlockCipher;
import com.google.bitcoin.core.EncryptedWallet.CbcAesBlockCipher.PasswordHash;

public class EncryptedWalletTest {

	@Test
	public void haveSomeUnEncryptedKeys_noEncryptedKeys() throws Exception{
		
	}
	
	@Test
	public void haveSomeEncryptedKeys_donNotSetCipherKey_unencrypt_catchException() throws Exception{
		
	}
	
	@Test
	public void createEncryptedWalletWithoutCipherkey_addUnencryptedKeys_serializeDeserialize_addEncryptedKey() 
		throws Exception{
		
	}
	
	@Test
	public void createEncryptedWallet_addKey_makeSure_addKey_EncryptedKey_isCalled() 
		throws Exception{
		
	}
	
	@Test
	public void encryptThenDecrypt_pass() throws Exception{
		CbcAesBlockCipher cipher = new CbcAesBlockCipher();
		
	  	byte[] pass = "smellyBobby".getBytes();
	  	byte[] salt = CbcAesBlockCipher.generateSalt();
	  	
	  	byte[] buf = cipher.encrypt(pass,salt, "my name is bobby simpson, I smell".getBytes());
	  	
	  	byte[] decryptBuf = cipher.decrypt(pass,salt, buf);
	  	
	  	assertThat(new String(decryptBuf),
	  			equalTo("my name is bobby simpson, I smell"));
	}
	
	@Test(expected=CryptoException.class)
	public void encryptThenDecrypt_fakePass_fail() throws Exception{
		CbcAesBlockCipher cipher = new CbcAesBlockCipher();
		
		byte[] plainData = "The world will one day undergo spahettification".getBytes("UTF-8");
		byte[] pass = "smellyBobby".getBytes();
		byte[] fakeBuf = "smellyBobbz".getBytes();
		byte[] salt = CbcAesBlockCipher.generateSalt();
		
		byte[] buf = cipher.encrypt(pass, salt, plainData);
		
		cipher.decrypt(fakeBuf, salt, buf);
	}
	
	@Test(expected=CryptoException.class)
	public void encryptThenDecrypt_fakeSalt() throws Exception{
		CbcAesBlockCipher cipher = new CbcAesBlockCipher();
		
		byte[] plainData = "Beleive in the one true god, FSM".getBytes();
		byte[] pass = "smellyBobby".getBytes();
		byte[] salt = CbcAesBlockCipher.generateSalt();
		byte[] fakeSalt = CbcAesBlockCipher.generateSalt();
		assertThat(salt,not(equalTo(fakeSalt)));
		
		byte[] buf = cipher.encrypt(pass, salt, plainData);
		cipher.decrypt(pass,fakeSalt,buf);
	}
	
	@Test
	public void passwordHash_valid() throws Exception{
		byte[] pass = "smellyBobby".getBytes();
		PasswordHash passwordHash = new PasswordHash(pass);
		println(passwordHash);
		assertTrue(passwordHash.passwordMatch(pass));
	}
	
	@Test 
	public void passwordHash_inValid() throws Exception{
		byte[] pass = "smellyBobby".getBytes();
		byte[] fakePass = "smdllyBobby".getBytes();
		PasswordHash passwordHash = new PasswordHash(pass);
		assertFalse(passwordHash.passwordMatch(fakePass));
	}
	
	static void println(Object ob){
		System.out.println(ob);
	}
}
