package com.cse5473.securegame.msg;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.cse5473.securegame.Encryption;

import it.unipr.ce.dsg.s2p.message.BasicMessage;
import it.unipr.ce.dsg.s2p.message.Payload;

/**
 * 
 * @author Caruso
 */
public final class VerificationMessage extends BasicMessage {
	/**
	 * 
	 */
	private static final String MSG_PEER_VERIFY = "peer_verify",
			PARAM_MESSAGE = "MESSAGE";

	/**
	 * 
	 */
	private static final Random RANDOM = new Random();

	/**
	 * 
	 * @param key
	 */
	public VerificationMessage(String key) {
		super(VerificationMessage.MSG_PEER_VERIFY, new Payload(
				VerificationMessage.generateParameters(key)));
	}

	/**
	 * 
	 * @param key
	 * @return
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
	 * 
	 * @param key
	 * @return
	 */
	public final boolean isValidKey(String key) {
		byte[] byteKey = null;
		try {
			byteKey = Encryption.GenerateAES128Key(key);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		byte[] message = (byte[]) this.getPayload().getParams()
				.get(VerificationMessage.PARAM_MESSAGE);
		try {
			Encryption.DecryptAES128(message, byteKey);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
