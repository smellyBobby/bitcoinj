package com.google.bitcoin.core;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AlgorithmParameters;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;

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
	
	static void println(Object ob){
		System.out.println(ob);
	}
	
	public static void main(String args[]) throws Exception{
	    
		KeyGenerator keyGen = null;
		
		SecretKeyFactory factory = 
			SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

		char[] password = "osethuosetuh".toCharArray();
		byte[] salt = new byte[8];
		int interations = 1000;
		KeySpec keySpec = new PBEKeySpec(password,salt,1024,256);
		SecretKey temp = factory.generateSecret(keySpec);
		SecretKey secret = new SecretKeySpec(temp.getEncoded(),"AES256");
		
		
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		
		cipher.init(Cipher.ENCRYPT_MODE, secret);
		AlgorithmParameters params = cipher.getParameters();
		byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
		byte[] ciphertext = cipher.doFinal("Hello, World!".getBytes("UTF-8"));

		println(cipher);
		println(ciphertext);
	}
}
