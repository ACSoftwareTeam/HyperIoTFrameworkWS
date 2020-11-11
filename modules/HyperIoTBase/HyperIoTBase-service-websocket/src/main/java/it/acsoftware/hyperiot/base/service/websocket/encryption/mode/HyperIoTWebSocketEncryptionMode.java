package it.acsoftware.hyperiot.base.service.websocket.encryption.mode;

import org.eclipse.jetty.websocket.api.Session;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * @Author Aristide Cittadino
 * Abstract Class identifying an Encryption Mode.
 */
public abstract class HyperIoTWebSocketEncryptionMode {
    /**
     * Init method for intializing mode
     *
     * @param s
     */
    public abstract void init(Session s);

    /**
     * Called on close
     *
     * @param s
     */
    public abstract void dispose(Session s);

    /**
     * Method used to update mode (new keys received or change algorithm)
     *
     * @param params
     */
    public abstract void update(Map<String, Object> params);

    /**
     * @return Current params of the encryption mode
     */
    public abstract Map<String, Object> getParams();

    /**
     * @return Current cipher for encryption
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidAlgorithmParameterException
     */
    public abstract Cipher getEncryptCipher() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException;

    /**
     * @return Current cipher for encryption
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public abstract Cipher getDecryptCipher() throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException;

    /**
     * Encrypts content
     *
     * @param plainText
     * @param encodeBase64
     * @return
     * @throws NoSuchPaddingException
     * @throws InvalidAlgorithmParameterException
     * @throws UnsupportedEncodingException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public abstract byte[] encrypt(byte[] plainText, boolean encodeBase64) throws NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException;

    /**
     * Decrypts content
     *
     * @param cipherText
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws UnsupportedEncodingException
     */
    public abstract byte[] decrypt(byte[] cipherText) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException;
}
