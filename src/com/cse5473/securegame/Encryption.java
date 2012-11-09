package com.cse5473.securegame;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * 
 * @author Caruso.66
 */
public final class Encryption {
	private static String KEY = "PASSWORD!!", MESSAGE = "HELLO";

	public static void main(String[] args) throws Exception {
		System.out.println("Hex MSG = "
				+ Encryption.asHex(Encryption.MESSAGE.getBytes()));
		System.out.println("Hex KEY = "
				+ Encryption.asHex(Encryption.KEY.getBytes()));

		byte[] key = Encryption.GenerateAES128Key(Encryption.KEY);

		System.out.println("Generated key based on [ " + KEY + " ]: "
				+ Encryption.asHex(key));

		byte[] encryption = Encryption.EncryptAES128(Encryption.MESSAGE, key);

		System.out.println("Generated encryption based on [ " + MESSAGE + " ]:"
				+ Encryption.asHex(encryption));

		byte[] decryption = Encryption.DecryptAES128(encryption, key);

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
		// Init the cipher with the given AES key.
		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

		// Initializations of message bytes, HMAC and final appended message.
		byte[] md5 = MessageDigest.getInstance("MD5")
				.digest(message.getBytes());
		byte[] msg = message.getBytes();
		byte[] finalMessage = new byte[message.getBytes().length + md5.length];

		// Append the message to the front of the final message.
		for (int i = 0; i < msg.length; i++)
			finalMessage[i] = msg[i];

		// Append the HMAC to the end of the final message.
		for (int i = msg.length; i < finalMessage.length; i++)
			finalMessage[i] = md5[i - msg.length];

		// Encrypt the message and return.
		return cipher.doFinal(finalMessage);
	}

	/**
	 * 
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public static byte[] GenerateAES128Key(String message)
			throws NoSuchAlgorithmException {
		// Return the MD5 of the input string.
		return MessageDigest.getInstance("MD5").digest(message.getBytes());
	}

	/**
	 * 
	 * @param encrypted
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public static byte[] DecryptAES128(byte[] encrypted, byte[] key)
			throws Exception {
		final int MD5_LENGTH = 16;

		// Init the cipher with the given AES key.
		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);

		// Initializations of decrypted message and empty byte arrays for
		// message and hmac.
		byte[] decrypted = cipher.doFinal(encrypted);
		byte[] message = new byte[decrypted.length - MD5_LENGTH];
		byte[] md5 = new byte[MD5_LENGTH];

		// Fill the HMAC
		for (int i = decrypted.length - MD5_LENGTH; i < decrypted.length; i++)
			md5[i - (decrypted.length - MD5_LENGTH)] = decrypted[i];

		// Fill the message
		for (int i = 0; i < message.length; i++)
			message[i] = decrypted[i];

		// Rehash the message as a check.
		byte[] md5Check = MessageDigest.getInstance("MD5").digest(message);

		// Compare all bytes in check to HMAC.
		for (int i = 0; i < md5.length; i++) {
			if (md5[i] != md5Check[i]) {
				throw new Exception("Invalid decryption detected, wrong key!");
			}
		}

		// Return the message iff HMAC dictates valid key.
		return message;
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
