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
 * This is the main class for encryption. It is a purely static class and should
 * NEVER be instantiated. To encrypt anything first generate a key based on some
 * arbitrary input string (Can be of any length). Then, this will generate the
 * MD5 hash of the input string, to be used for the encryption. After it is
 * encrypted, the key will need to be stored to call decrypt in the future and
 * regain access to the data.
 * 
 * @author Caruso.66
 */
public final class Encryption {
	/**
	 * This is the main encryption workhorse for our game. It takes in a string
	 * and then easily encrypts it using whatever 128bit byte array was passed.
	 * 
	 * @param message
	 *            The message to be encrypted.
	 * @param key
	 *            The 128-bit byte array representing the key.
	 * @return A byte[] representing the encrypted value.
	 * @throws Exception
	 *             Only thrown when the key is invalid for any reason (length,
	 *             etc)...
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
	 * Takes in a string and returns a byte[] representing the MD5 hash of the
	 * value.
	 * 
	 * @return The MD5 byte[] of the value.
	 * @throws NoSuchAlgorithmException
	 *             Never thrown, against ensures clause.
	 */
	public static byte[] GenerateAES128Key(String message)
			throws NoSuchAlgorithmException {
		// Return the MD5 of the input string.
		return MessageDigest.getInstance("MD5").digest(message.getBytes());
	}

	/**
	 * Decrypts the encrypted message based on whatever key is input into the
	 * key field. If the key is not, in fact, the key that was used to encrypt
	 * the string then this will exit due to exceptions.
	 * 
	 * @param encrypted
	 *            The encrypted byte array.
	 * @param key
	 *            The correct 128-bit key.
	 * @return The byte[] of the original message.
	 * @throws Exception
	 *             Iff the key is invalid (As determined by our HMAC).
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
	 * Pretty Print for byte arrays.
	 * 
	 * @param buf
	 *            The byte[] to be printed as hexidecimal.
	 * @return The String of the byte[] buf.
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
