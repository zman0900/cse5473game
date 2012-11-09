package com.cse5473.securegame;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * 
 * @author Caruso.66
 */
public final class Encryption {
	private static String KEY = "PASSWORD!!", MESSAGE = "HELLO";

	public static void main(String[] args) throws NoSuchAlgorithmException,
			InvalidKeyException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException {
		byte[] key = Encryption.GenerateAES128Key(Encryption.KEY);
		byte[] encryption = Encryption.EncryptAES128(Encryption.MESSAGE, key);
		byte[] decryption = Encryption.DecryptAES128(encryption, key);
		System.out.println("Generated key based on [ " + KEY + " ]: "
				+ Encryption.asHex(key));
		System.out.println("Generated encryption based on [ " + MESSAGE + " ]:"
				+ Encryption.asHex(encryption));
		System.out.println("Decrypted as:" + Encryption.asHex(decryption));
	}

	/**
	 * 
	 * @param message
	 * @param key
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public static byte[] EncryptAES128(String message, byte[] key)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

		Cipher cipher = Cipher.getInstance("AES");

		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

		return cipher.doFinal(message.getBytes());
	}

	/**
	 * 
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public static byte[] GenerateAES128Key(String message)
			throws NoSuchAlgorithmException {
		byte[] md5 = MessageDigest.getInstance("MD5")
				.digest(message.getBytes());
		byte[] paddedMD5 = new byte[128];
		for (int i = 0; i < paddedMD5.length; i++) {
			paddedMD5[i] = md5[i % md5.length];
		}
		return paddedMD5;
	}

	/**
	 * 
	 * @param encrypted
	 * @param key
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public static byte[] DecryptAES128(byte[] encrypted, byte[] key)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

		Cipher cipher = Cipher.getInstance("AES");

		cipher.init(Cipher.DECRYPT_MODE, skeySpec);

		return cipher.doFinal(encrypted);
	}

	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static String asHex(byte buf[]) {

		StringBuffer strbuf = new StringBuffer(buf.length * 2);
		int i;

		for (i = 0; i < buf.length; i++) {
			if (((int) buf[i] & 0xff) < 0x10)
				strbuf.append("0");

			strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
		}

		return strbuf.toString();
	}
}
