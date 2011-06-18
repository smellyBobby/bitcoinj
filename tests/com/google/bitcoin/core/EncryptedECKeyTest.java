package com.google.bitcoin.core;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static com.google.bitcoin.core.Utils.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AlgorithmParameters;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;

import com.google.bitcoin.bouncycastle.crypto.CipherParameters;
import com.google.bitcoin.bouncycastle.crypto.DataLengthException;
import com.google.bitcoin.bouncycastle.crypto.InvalidCipherTextException;
import com.google.bitcoin.bouncycastle.crypto.PBEParametersGenerator;
import com.google.bitcoin.bouncycastle.crypto.digests.SHA256Digest;
import com.google.bitcoin.bouncycastle.crypto.engines.AESEngine;
import com.google.bitcoin.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import com.google.bitcoin.bouncycastle.crypto.modes.CBCBlockCipher;
import com.google.bitcoin.bouncycastle.crypto.paddings.PKCS7Padding;
import com.google.bitcoin.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import com.google.bitcoin.bouncycastle.crypto.params.KeyParameter;
import com.google.bitcoin.bouncycastle.crypto.params.ParametersWithIV;
import com.google.bitcoin.core.EncryptedKeysWallet.CbcAesBlockCipher;

public class EncryptedECKeyTest {

	@Test
	public void readObject() throws Exception{
		EncryptedECKey eEcKey = new EncryptedECKey();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(eEcKey);
		
		byte[] b = baos.toByteArray();
		ByteArrayInputStream bais = new ByteArrayInputStream(b);
		ObjectInputStream ois = new ObjectInputStream(bais);
		EncryptedECKey eEcKeyResult = (EncryptedECKey) ois.readObject();
		
		println(eEcKey.toString());
		println(eEcKeyResult.toString());
		assertThat(eEcKeyResult.toString(),equalTo(eEcKey.toString()));
	}
	
	
	@Test
	public void encryptThenDecrypt_pass() throws Exception{
		CbcAesBlockCipher cipher = new CbcAesBlockCipher();
		
	  	byte[] pass = "micheal".getBytes();
	  	byte[] salt = CbcAesBlockCipher.generateSalt();
	  	
	  	byte[] buf = cipher.encrypt(pass,salt, "my name is bobby simpson, I smell".getBytes());
	  	
	  	
	  	byte[] decryptBuf = cipher.decrypt(pass,salt, buf);
	  	
	  	assertThat(new String(decryptBuf),
	  			equalTo("my name is bobby simpson, I smell"));
	}
	static void println(Object ob){
		System.out.println(ob);
	}

}
