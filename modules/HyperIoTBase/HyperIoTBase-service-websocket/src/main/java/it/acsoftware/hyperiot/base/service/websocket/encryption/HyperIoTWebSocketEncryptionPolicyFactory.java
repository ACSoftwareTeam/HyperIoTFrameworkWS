package it.acsoftware.hyperiot.base.service.websocket.encryption;

import it.acsoftware.hyperiot.base.service.websocket.encryption.mode.HyperIoTRSAWithAESEncryptionMode;
import it.acsoftware.hyperiot.base.service.websocket.encryption.policy.HyperIoTWebSocketEncryptionPolicy;

/**
 * @Author Aristide Cittadino
 * Factory for creating alla available Encryption Policies for websockets
 */
public class HyperIoTWebSocketEncryptionPolicyFactory {

    /**
     * @return
     */
    public static HyperIoTWebSocketEncryptionPolicy createRSAAndAESEncryptionPolicy() {
        HyperIoTRSAWithAESEncryptionMode rsaAndAesMode = new HyperIoTRSAWithAESEncryptionMode();
        HyperIoTWebSocketEncryptionPolicy rsaAndAes = new HyperIoTWebSocketEncryptionPolicy(rsaAndAesMode);
        return rsaAndAes;
    }
}
