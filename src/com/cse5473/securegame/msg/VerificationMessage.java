package com.cse5473.securegame.msg;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.cse5473.securegame.Encryption;

import it.unipr.ce.dsg.s2p.message.BasicMessage;
import it.unipr.ce.dsg.s2p.message.Payload;

/**
 * The verification message is a message that allows the validity of any input
 * key to be validated without literally sending the key as plaintext through
 * any sort of message medium (as that is insecure). Instead, both users are to
 * have a pre-agreed upon password that they can both enter, the first user to
 * generate this verification message, the second user to authenticate this
 * verification message.
 * 
 * @author Caruso
 */
public final class VerificationMessage extends BasicMessage {
	/**
	 * The param indentifiers and main identifier for the message.
	 */
	private static final String MSG_PEER_VERIFY = "peer_verify",
			PARAM_MESSAGE = "MESSAGE";

	/**
	 * The random number generator for use in all instances of the verification
	 * message. (IF we don't make it static it wouldn't be random).
	 */
	private static final Random RANDOM = new Random();

	/**
	 * The verification message only needs a key to be generated. It'll then
	 * generate an encryption of some random integer, however because the
	 * encryption also has an HMAC the decryption is verifiable based on
	 * whatever the encryption turned out to be. If the key is invalid we know
	 * the user was not well informed on a key or an attack has been attempted.
	 * 
	 * @param key
	 *            The key to generate the message.
	 */
	public VerificationMessage(String key) {
		super(VerificationMessage.MSG_PEER_VERIFY, new Payload(
				VerificationMessage.generateParameters(key)));
	}

	/**
	 * Encrpyts a random integer (0-MAX_VALUE) with the given key. This allows
	 * the person on the other end to attempt to decrypt it with a key that they
	 * will already have to know.
	 * 
	 * @param key
	 *            The key to encrypt the random message with.
	 * @return The Map holding the random encrypted message for the payload.
	 */
	private final static Map<String, Object> generateParameters(String key) {
		Map<String, Object> map = new HashMap<String, Object>(0);
		byte[] byteKey;
		try {
			byteKey = Encryption.GenerateAES128Key(key);
			map.put(PARAM_MESSAGE, Encryption.EncryptAES128(""
					+ VerificationMessage.RANDOM.nextInt(Integer.MAX_VALUE),
					byteKey));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return map;
	}

	/**
	 * Returns true iff the key allows for the stored message to be decrypted
	 * correctly (detected by the HMAC) if the key is not valid, this will
	 * return false.
	 * 
	 * @param key The key to test.
	 * @return True iff the key is correct for the stored encryption.
	 */
	public static final boolean isValidKey(byte[] message, String key) {
		byte[] byteKey = null;
		try {
			byteKey = Encryption.GenerateAES128Key(key);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		try {
			Encryption.DecryptAES128(message, byteKey);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
