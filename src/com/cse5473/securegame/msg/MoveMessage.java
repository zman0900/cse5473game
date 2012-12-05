package com.cse5473.securegame.msg;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.cse5473.securegame.Encryption;
import com.cse5473.securegame.GameView;

import it.unipr.ce.dsg.s2p.message.BasicMessage;
import it.unipr.ce.dsg.s2p.message.Payload;
import it.unipr.ce.dsg.s2p.peer.PeerDescriptor;

/**
 * Handles the message format for any moves made on the board.
 * 
 * @author Caruso
 * 
 */
public final class MoveMessage extends BasicMessage {
	/**
	 * The different payload labels.
	 */
	private static final String MSG_PEER_MOVE = "peer_move",
			PARAM_STATE = "STATE", PARAM_INDEX = "INDEX";

	/**
	 * Creates a new move message given a state and an index to change the
	 * gameview on the other player's screen. The peer descriptor is the PD of
	 * the person sending the message and the key, while not stored in the
	 * message, is needed to encrypt the message before it is sent.
	 * 
	 * @param state
	 *            The state to be updated to.
	 * @param index
	 *            The index that it is to be placed at on the board (0-9)
	 * @param key
	 *            The key to encrypt with.
	 */
	public MoveMessage(GameView.State state, int index, String key) {
		super(MoveMessage.MSG_PEER_MOVE, new Payload(generateParamMap(state,
				index, key)));
	}

	/**
	 * Generates the parameter map given the PeerDescriptor, state and index it
	 * needs to store. It then proceeds to encrypt both the state and the index
	 * separately and store them in the payload map along with the
	 * peerdescriptor.
	 * 
	 * @param peer
	 *            The peer descriptor of the one sending the message.
	 * @param state
	 *            The state that is to be updated.
	 * @param index
	 *            The index at which to update the state.
	 * @param key
	 *            The key to encrypt both the index and state with.
	 * @return The map containing the encrypted data.
	 */
	private static final Map<String, Object> generateParamMap(
			GameView.State state, int index, String key) {
		Map<String, Object> params = new HashMap<String, Object>(0);
		byte[] byteKey = null;
		try {
			byteKey = Encryption.GenerateAES128Key(key);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}

		try {
			params.put(MoveMessage.PARAM_STATE,
					Encryption.EncryptAES128(state.toString(), byteKey));
			params.put(MoveMessage.PARAM_INDEX,
					Encryption.EncryptAES128(Integer.toString(index), byteKey));
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return null;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
			return null;
		} catch (BadPaddingException e) {
			e.printStackTrace();
			return null;
		}
		return params;
	}

	/**
	 * Returns the decrypted state iff the key is valid (The key should always
	 * be valid at this point due to the verification messages, if it isn't
	 * valid then the message simply won't return a value from its payload.
	 * 
	 * @param key
	 *            The key to decrypt the message with.
	 * @return The state that was stored in the original payload.
	 */
	public final GameView.State getDecryptedState(String key) {
		byte[] msg = (byte[]) this.getPayload().getParams()
				.get(MoveMessage.PARAM_STATE);
		byte[] byteKey = null;
		try {
			byteKey = Encryption.GenerateAES128Key(key);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		try {
			return GameView.State.valueOf(new String(Encryption.DecryptAES128(
					msg, byteKey)));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns the decrypted index iff the key is valid (The key should always
	 * be valid at this point due to the verification messages, if it isn't
	 * valid then the message simply won't return a value from its payload.
	 * 
	 * @param key
	 *            The key to be used to access the information in the payload.
	 * @return The decrypted index from the payload, or null iff the key is
	 *         wrong.
	 */
	public final Integer getDecryptedIndex(String key) {
		byte[] msg = (byte[]) this.getPayload().getParams()
				.get(MoveMessage.PARAM_INDEX);
		byte[] byteKey = null;
		try {
			byteKey = Encryption.GenerateAES128Key(key);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		try {
			return Integer.valueOf(new String(Encryption.DecryptAES128(msg,
					byteKey)));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
