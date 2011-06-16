package com.google.bitcoin.core;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

import com.google.bitcoin.bouncycastle.crypto.CryptoException;
import com.google.bitcoin.core.EncryptedWallet.CbcAesBlockCipher;
import com.google.bitcoin.core.EncryptedWallet.PasswordHash;

public class EncryptedWalletTest {
	
	//Production 
	NetworkParameters np;
	
	File walletFile = new File("encryptedWallet");
	
	byte[] password = "password".getBytes();
	
	byte[] wrongPassword = "passwore".getBytes();
	
	@Test
	public void create() throws Exception{
		EncryptedWallet wallet = EncryptedWallet.newEncryptedWallet(np,walletFile);
		assertThat(wallet,notNullValue());
	}
	
	@Test
	public void createEncryptedKey() throws Exception{
		EncryptedWallet wallet = EncryptedWallet.newEncryptedWallet(np, walletFile);
		ECKey ecKey = wallet.createKey(true);
		
		assertThat( ecKey.isEncrypted() , equalTo( true ) );
		assertThat( wallet.hasEncryptedKeys() , equalTo( true ) );
		assertThat( wallet.removeEncryptedKeys().size() , equalTo(1) );
		
	}
	
	@Test
	public void createUnencryptedKey() throws Exception{
		EncryptedWallet wallet = EncryptedWallet.newEncryptedWallet(np,walletFile);
		ECKey ecKey = wallet.createKey(false);
		
		assertThat( ecKey.isEncrypted() , equalTo( false ) );
		assertThat( wallet.hasEncryptedKeys() , equalTo( false ) );
		assertThat( wallet.removeEncryptedKeys().size() , equalTo( 0 ) );
	}
	
	@Test
	public void createEncryptedKey_thenAddToWallet() throws Exception{
		EncryptedWallet wallet = EncryptedWallet.newEncryptedWallet(np, walletFile);
		ECKey ecKey = new ECKey(true);
		wallet.addKey(ecKey);
		
		assertThat( wallet.hasEncryptedKeys() , equalTo( true ) );
		assertThat( wallet.removeEncryptedKeys().size() , equalTo(1) );
		assertThat( wallet.removeEncryptedKeys().size() , equalTo(0) );
	}
	
	@Test
	public void createEncryptedKeys_thenEncrypt() throws Exception {
		EncryptedWallet wallet = EncryptedWallet.newEncryptedWallet(np, walletFile);
		
		//Case where encryption is necessary.
		wallet.createKey(true);
		wallet.createKey(true);
		wallet.encryptAndSerialize(password);

		//Case where encryption is not necessary.
		wallet.removeEncryptedKeys();
		wallet.serialize();
	}
	
	@Test
	public void encrypt_decrypt() throws Exception {
		EncryptedWallet wallet = EncryptedWallet.newEncryptedWallet(np, walletFile);
		
		wallet.createKey();
		
		assertThat( wallet.hasEncryptedKeys() , equalTo( false ) );
		
		wallet.serialize();
		wallet = EncryptedWallet.loadFromFile(walletFile);
		assertThat( wallet.keyDecryptionNeeded() , equalTo( false ) );
		
		assertThat( wallet.keychain.size(), equalTo( 1 ) );
	}
	
	@Test
	public void encrypt_decrypt_withEncryptedKeys() throws Exception{
		EncryptedWallet wallet = EncryptedWallet.newEncryptedWallet(np, walletFile);
		
		wallet.createKey(true);
		wallet.createKey(true);
		
		assertThat( wallet.hasEncryptedKeys() , equalTo( true ) );
		
		wallet.encryptAndSerialize(password);
		
		assertThat( wallet.keychain.size() , equalTo(2) );
		
		wallet = EncryptedWallet.loadFromFile(walletFile);
		assertThat( wallet.keyDecryptionNeeded() , equalTo( true ) );
		assertThat( wallet.removeEncryptedKeys().size() , equalTo( 0 ) );
		assertThat( wallet.keychain.size() , equalTo( 0 ) );
		
		wallet.decryptKeys(password);
		
		assertThat( wallet.removeEncryptedKeys().size() , equalTo(2) );
	}
	
	@Test(expected=CryptoException.class)
	public void someEncryptedKeys_encrypt_decryptUseWrongPass() throws Exception{
		EncryptedWallet wallet = EncryptedWallet.newEncryptedWallet(np, walletFile);
		
		wallet.createKey(true);
		wallet.createKey(true);
		wallet.createKey();
		
		assertThat( wallet.hasEncryptedKeys() , equalTo( true ) );
		
		wallet.encryptAndSerialize(password);
		
		wallet = EncryptedWallet.loadFromFile(walletFile);
		
		assertThat(wallet.keychain.size() , equalTo( 1 ) );
		assertThat(wallet.keyDecryptionNeeded() , equalTo( true ) );
		
		wallet.decryptKeys(wrongPassword);
	}
	
	@Test(expected=CryptoException.class)
	public void someEncryptedKeys_encrypt_encryptAgainDiffPass() throws Exception{
		EncryptedWallet wallet = EncryptedWallet.newEncryptedWallet(np, walletFile);
		
		wallet.createKey(true);
		wallet.createKey(true);
		
		wallet.encryptAndSerialize(password);
		wallet.saveToFile(walletFile, wrongPassword);
		
		wallet = EncryptedWallet.loadFromFile(walletFile);
		assertThat(wallet.removeEncryptedKeys().size() , equalTo( 0 ) );
		wallet.decryptKeys(wrongPassword);
		
		assertThat( wallet.keychain.size() , equalTo( 2 ) );
		
		wallet = EncryptedWallet.loadFromFile(walletFile);
		
		assertThat( wallet.keyDecryptionNeeded() , equalTo( true ) );
		
		wallet.decryptKeys(password);
	}
	
	@Test
	public void someEncryptedKeys_encryptDecrypt_removeKeys() throws Exception{
		EncryptedWallet wallet = EncryptedWallet.newEncryptedWallet( np , walletFile );
		
		wallet.createKey(true);
		wallet.createKey(true);
		wallet.encryptAndSerialize(password);
		wallet.removeEncryptedKeys();
		assertThat( wallet.keychain.size() , equalTo( 0 ) );
		wallet.encryptAndSerialize(password);
		
		wallet = EncryptedWallet.loadFromFile(walletFile);
		
		assertThat( wallet.keyDecryptionNeeded() , equalTo( false ) );
		
		wallet.createKey(true);
		wallet.createKey(true);
		wallet.encryptAndSerialize(password);
		
		wallet.removeEncryptedKeys();
		assertThat( wallet.keychain.size() , equalTo( 0 ) );
		
		wallet.decryptKeys(password);
		assertThat( wallet.keychain.size() , equalTo( 2 ) );
	}
	@Test
	public void passwordAttempts() throws Exception {
		EncryptedWallet wallet = EncryptedWallet.newEncryptedWallet(np, walletFile);
		
		wallet.createKey(true);
		wallet.createKey();
		
		wallet.encryptAndSerialize(password);
		wallet = EncryptedWallet.loadFromFile(walletFile);
		
		assertThat( wallet.keychain.size() , equalTo( 1 ) );
		assertThat( wallet.keyDecryptionNeeded() , equalTo( true ) );
		
		try{
			wallet.decryptKeys(wrongPassword);
		} catch (Exception e){
			println(e);
		}
		
		wallet.decryptKeys(password);
		
		assertThat( wallet.keychain.size() , equalTo( 2 ));
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
	public void encryptDecrypt_diffCiphers() throws Exception{
		CbcAesBlockCipher cipher1 = new CbcAesBlockCipher();
		byte[] plainData = "plainData".getBytes();
		byte[] salt = CbcAesBlockCipher.generateSalt();
		
		byte[] buf = cipher1.encrypt(password, salt, plainData);
		
		cipher1 = new CbcAesBlockCipher();
		cipher1.decrypt(password, salt, buf);
	}
	
	@Test
	public void testParametersWithIV() throws Exception{
		byte[] salt = CbcAesBlockCipher.generateSalt();
		CbcAesBlockCipher cipher1 = new CbcAesBlockCipher();
		CbcAesBlockCipher cipher2 = new CbcAesBlockCipher();
		
		byte[] plainData = "plainData".getBytes();
		
		byte[] buf1 = cipher1.encrypt(password, salt, plainData);
		byte[] buf2 = cipher2.encrypt(password, salt, plainData);
		
		assertThat(buf1,equalTo(buf2));
		
		byte[] res1from2 = cipher1.decrypt(password, salt, buf2);
		byte[] res2from1 = cipher2.decrypt(password, salt, buf1);
		
		assertThat(res1from2,equalTo(res2from1));
		
		CbcAesBlockCipher cipher3 = new CbcAesBlockCipher();
		
		byte[] res3 = cipher3.decrypt(password, salt, buf2);
		
		println(new String(res3));
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
	
	@Before
	public void beforeTest() throws IOException {
		np = NetworkParameters.prodNet();
		if(walletFile.exists())walletFile.delete();
		walletFile.createNewFile();
	}
	static void println(Object ob){
		System.out.println(ob);
	}
}
