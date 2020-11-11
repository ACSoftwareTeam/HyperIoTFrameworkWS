package it.acsoftware.hyperiot.base.service.websocket.encryption.policy;

import it.acsoftware.hyperiot.base.service.websocket.encryption.mode.HyperIoTWebSocketEncryptionMode;
import org.eclipse.jetty.websocket.api.Session;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Author Aristide Cittadino
 * This class maps the concept of encryption policy, It owns an encryption mode which is responsable
 * of how messages are encrypted or decrypted.
 */
public class HyperIoTWebSocketEncryptionPolicy {

    private HyperIoTWebSocketEncryptionMode mode;

    public HyperIoTWebSocketEncryptionPolicy(HyperIoTWebSocketEncryptionMode mode) {
        this.mode = mode;
    }

    public byte[] encrypt(byte[] message, boolean encodeBase64) throws NoSuchPaddingException, InvalidKeyException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        return mode.encrypt(message, encodeBase64);
    }

    public byte[] decrypt(byte[] message, boolean decodeBase64) throws NoSuchPaddingException, InvalidKeyException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        return mode.decrypt(message);
    }

    public void updateMode(Map<String, Object> params) {
        mode.update(params);
    }

    public Map<String, Object> getModeParams() {
        return mode.getParams();
    }

    public void init(Session s) {
        mode.init(s);
    }

    public void dispose(Session s) {
        mode.dispose(s);
    }

}
