package it.acsoftware.hyperiot.base.service.websocket.encryption.mode;

import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.*;

/**
 * @Author Aristide Cittadino
 * MixedEncryptionMode rappresents symmetric and asymmetric encryption methods combined.
 * In the first phase, client will exchange their symmetric password using an asymmetric encryption.
 * Then the communication will continuo with symmetric cryptography.
 */
public abstract class HyperIoTWebSocketMixedEncryptionMode extends HyperIoTWebSocketEncryptionMode {
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private byte[] symmetricPassword;
    private byte[] symmetricIv;

    /**
     * @return
     */
    public Key getPublicKey() {
        return publicKey;
    }

    /**
     * @param publicKey
     */
    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    /**
     * @return
     */
    public Key getPrivateKey() {
        return privateKey;
    }

    /**
     * @param privateKey
     */
    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * @return
     */
    public byte[] getSymmetricPassword() {
        return symmetricPassword;
    }

    /**
     * @param symmetricPassword
     */
    public void setSymmetricPassword(byte[] symmetricPassword) {
        this.symmetricPassword = symmetricPassword;
    }

    /**
     * @return
     */
    public byte[] getSymmetricIv() {
        return symmetricIv;
    }

    /**
     * @param symmetricIv
     */
    public void setSymmetricIv(byte[] symmetricIv) {
        this.symmetricIv = symmetricIv;
    }

    /**
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
    @Override
    public byte[] encrypt(byte[] plainText, boolean encodeBase64) throws NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if (this.symmetricPassword == null) {
            return HyperIoTSecurityUtil.encryptText(this.publicKey, plainText, encodeBase64, this.getEncryptCipher());
        } else {
            return HyperIoTSecurityUtil.encryptWithAES(symmetricPassword, symmetricIv, new String(plainText), this.getEncryptCipher());
        }
    }

    /**
     * @param plainText
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws UnsupportedEncodingException
     */
    @Override
    public byte[] decrypt(byte[] plainText) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        if (this.symmetricPassword == null) {
            return HyperIoTSecurityUtil.decodeMessageWithPrivateKey(this.privateKey, plainText, this.getDecryptCipher());
        } else {
            return HyperIoTSecurityUtil.decryptWithAES(symmetricPassword, symmetricIv, new String(plainText), this.getDecryptCipher());
        }
    }
}
