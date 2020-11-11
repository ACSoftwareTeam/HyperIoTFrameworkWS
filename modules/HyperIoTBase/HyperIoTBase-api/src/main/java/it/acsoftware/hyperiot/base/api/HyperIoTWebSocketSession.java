package it.acsoftware.hyperiot.base.api;

import org.eclipse.jetty.websocket.api.Session;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Author Aristide Cittadino
 */
public interface HyperIoTWebSocketSession {

    /**
     * @return true if this websocket requires authentication
     */
    boolean isAuthenticationRequired();

    /**
     * Method which implements authentication for websocket
     */
    void authenticate();

    /**
     * @return WebSocket Session
     */
    Session getSession();

    /**
     * Use this method to insert custom initialization code, on websocket open event
     */
    void initialize();

    /**
     * @param message
     */
    void onMessage(String message);

    /**
     *
     */
    void dispose();

    /**
     * @return Web Socket Policy Params to check
     */
    default Map<String, Object> getPolicyParams() {
        return null;
    }

    /**
     * @return The Policy List for the created websocket instance
     */
    default List<HyperIoTWebSocketPolicy> getWebScoketPolicies() {
        return Collections.emptyList();
    }
}
