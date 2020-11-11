package it.acsoftware.hyperiot.base.service.websocket.bridge;

import it.acsoftware.hyperiot.base.api.HyperIoTWebSocketEndPoint;
import it.acsoftware.hyperiot.base.api.HyperIoTWebSocketSession;
import org.eclipse.jetty.websocket.api.Session;

/**
 * Example how to user a bridge endpoing
 * it was used for testing purpose
 *
 * @Component(immediate = true)
 */
public class HyperIoTWebSocketBridgeEndpoint implements HyperIoTWebSocketEndPoint {
    /**
     * Gets the relative path name of this WebSocket endpoint
     *
     * @return The path name
     */
    public String getPath() {
        return "ws-bridge";
    }

    /**
     * Get the WebSocket handler for a given session
     *
     * @param session The session instance
     * @return The WebSocket session handler
     */
    public HyperIoTWebSocketSession getHandler(Session session) {
        //forcing not authenticated at staring up but auth is enforced later in WebSocketService
        return new HyperIoTWebSocketGroupedBridgeSession(session, false);
    }


}
