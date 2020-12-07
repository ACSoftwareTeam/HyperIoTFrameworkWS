package it.acsoftware.hyperiot.base.service.websocket.policy;

import it.acsoftware.hyperiot.base.model.HyperIoTWebSocketAbstractPolicy;
import org.eclipse.jetty.websocket.api.Session;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MaxPayloadBytesPolicy extends HyperIoTWebSocketAbstractPolicy {
    private static Logger log = Logger.getLogger(MaxPayloadBytesPolicy.class.getName());
    private long maxPayloadBytes;

    public MaxPayloadBytesPolicy(Session s, long maxPayloadBytes) {
        super(s);
        this.maxPayloadBytes = maxPayloadBytes;
    }

    @Override
    public boolean closeWebSocketOnFail() {
        return false;
    }

    @Override
    public boolean printWarningOnFail() {
        return true;
    }

    @Override
    public boolean sendWarningBackToClientOnFail() {
        return true;
    }

    @Override
    public boolean ignoreMessageOnFail() {
        return true;
    }

    @Override
    public boolean isSatisfied(Map<String, Object> params, byte[] payload) {
        log.log(Level.FINE, "Policy Max Payload bytes, current payload is: {0}, max is {1}", new Object[]{payload.length, maxPayloadBytes});
        if (payload.length > maxPayloadBytes) {
            return false;
        }
        return true;
    }
}
