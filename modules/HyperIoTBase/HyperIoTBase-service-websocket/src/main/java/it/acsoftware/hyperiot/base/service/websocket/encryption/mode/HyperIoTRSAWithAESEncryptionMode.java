package it.acsoftware.hyperiot.base.service.websocket.encryption.mode;

import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import org.eclipse.jetty.websocket.api.Session;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HyperIoTRSAWithAESEncryptionMode extends HyperIoTWebSocketMixedEncryptionMode {
    private static Logger log = Logger.getLogger("it.acsoftware.hyperiot");

    public static final String MODE_PARAM_AES_PASSWORD = "aesPassword";
    public static final String MODE_PARAM_AES_IV = "aesIv";

    protected static final ThreadLocal<Cipher> cipherRSADecWeb = new ThreadLocal<Cipher>() {
        @Override
        protected Cipher initialValue() {
            try {
                return HyperIoTSecurityUtil.getCipherRSAPKCS1Padding(true);
            } catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
            return null;
        }
    };
    protected static ThreadLocal<Cipher> cipherRSAEncWeb = new ThreadLocal<Cipher>() {
        @Override
        protected Cipher initialValue() {
            try {
                return HyperIoTSecurityUtil.getCipherRSAPKCS1Padding(true);
            } catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
            return null;
        }
    };
    ;
    protected static ThreadLocal<Cipher> cipherRSADec = new ThreadLocal<Cipher>() {
        @Override
        protected Cipher initialValue() {
            try {
                return HyperIoTSecurityUtil.getCipherRSAOAEPPAdding();
            } catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
            return null;
        }
    };

    protected static ThreadLocal<Cipher> cipherRSAEnc = new ThreadLocal<Cipher>() {
        @Override
        protected Cipher initialValue() {
            try {
                return HyperIoTSecurityUtil.getCipherRSAOAEPPAdding();
            } catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
            return null;
        }
    };

    private Cipher currCipherRSADec;
    private Cipher currCipherRSAEnc;
    private Cipher currAesCipherEnc;
    private Cipher currAesCipherDec;

    @Override
    public void init(Session session) {
        try {
            String clientPubKeyStrEnc = session.getUpgradeRequest().getHeader(("X-HYPERIOT-CLIENT-PUB-KEY"));
            //Using OAEP Padding

            if (clientPubKeyStrEnc == null) {
                //trying with query param since javascript API doesn't support custom headers in websocket
                List<String> pubKeyParam = session.getUpgradeRequest().getParameterMap().get("hyperiot-client-pub-key");
                if (pubKeyParam.size() == 1) {
                    clientPubKeyStrEnc = pubKeyParam.get(0);
                }
                this.currCipherRSADec = this.cipherRSADecWeb.get();
                this.currCipherRSAEnc = this.cipherRSAEncWeb.get();
            } else {
                this.currCipherRSADec = this.cipherRSADec.get();
                this.currCipherRSAEnc = this.cipherRSADec.get();
            }
            if (clientPubKeyStrEnc != null) {
                byte[] decodedPubKey = Base64.getDecoder().decode(clientPubKeyStrEnc.getBytes("UTF8"));
                byte[] decryptedPubKey = HyperIoTSecurityUtil.decodeMessageWithServerPrivateKey(decodedPubKey, this.currCipherRSADec);
                String clientPubKeyStr = new String(decryptedPubKey);
                PublicKey clientPubKey = HyperIoTSecurityUtil.getPublicKeyFromString(clientPubKeyStr);
                this.setPublicKey(clientPubKey);
                this.setPrivateKey(HyperIoTSecurityUtil.getServerKeyPair().getPrivate());
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public void dispose(Session s) {
        //do nothing
    }

    @Override
    public Cipher getEncryptCipher() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        if (this.getSymmetricPassword() == null)
            return this.currCipherRSAEnc;
        else if (this.currAesCipherEnc == null) {
            this.currAesCipherEnc = HyperIoTSecurityUtil.getCipherAES();
            SecretKeySpec skeySpec = new SecretKeySpec(this.getSymmetricPassword(), "AES");
            IvParameterSpec iv = null;
            if (this.getSymmetricIv() != null)
                iv = new IvParameterSpec(this.getSymmetricIv());

            if (iv != null)
                this.currAesCipherEnc.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            else
                this.currAesCipherEnc.init(Cipher.ENCRYPT_MODE, skeySpec);
        }
        return currAesCipherEnc;
    }

    @Override
    public Cipher getDecryptCipher() throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
        if (this.getSymmetricPassword() == null)
            return this.currCipherRSADec;
        else if (this.currAesCipherDec == null) {
            this.currAesCipherDec = HyperIoTSecurityUtil.getCipherAES();
            SecretKeySpec skeySpec = new SecretKeySpec(this.getSymmetricPassword(), "AES");
            IvParameterSpec iv = null;
            if (this.getSymmetricIv() != null)
                iv = new IvParameterSpec(this.getSymmetricIv());

            if (iv != null)
                this.currAesCipherDec.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            else
                this.currAesCipherDec.init(Cipher.DECRYPT_MODE, skeySpec);
        }
        return currAesCipherDec;
    }

    @Override
    public void update(Map<String, Object> params) {
        byte[] aesPassword = (byte[]) params.get(MODE_PARAM_AES_PASSWORD);
        byte[] iv = (byte[]) params.get(MODE_PARAM_AES_IV);
        this.setSymmetricPassword(aesPassword);
        this.setSymmetricIv(iv);
    }

    @Override
    public Map<String, Object> getParams() {
        HashMap<String, Object> params = new HashMap<>();
        params.put(MODE_PARAM_AES_PASSWORD, this.getSymmetricPassword());
        params.put(MODE_PARAM_AES_IV, this.getSymmetricIv());
        return params;
    }
}
