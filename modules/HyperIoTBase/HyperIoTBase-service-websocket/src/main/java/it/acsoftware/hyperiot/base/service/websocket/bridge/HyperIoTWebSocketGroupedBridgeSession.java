package it.acsoftware.hyperiot.base.service.websocket.bridge;

import it.acsoftware.hyperiot.base.model.HyperIoTWebSocketMessage;
import it.acsoftware.hyperiot.base.model.HyperIoTWebSocketMessageType;
import it.acsoftware.hyperiot.base.service.websocket.compression.HyperIoTWebSocketCompressionPolicy;
import it.acsoftware.hyperiot.base.service.websocket.encryption.policy.HyperIoTWebSocketEncryptionPolicy;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import org.eclipse.jetty.websocket.api.Session;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Author Aristide Cittadino.
 * Bridged sessions are 2 web socket sessions comunicating each other as a single channel, used in realtime communication
 * between 2 clients that cannot talk each othern directly or in different ways.
 * We define two actors:
 * - The websocket owner: user who opens first the connection and define the key that must be used for other users to connect to the same session
 * - The partecipant: user who wants to partecipate to a websocket session that must specify the same key of the owner who wants to be connected to
 * <p>
 * Grouped Bridge Session are session between 2 clients which are managed on a single environment.
 * Even it is clustered grouped session can be implemented with a load balancer.
 * the LB groups sessions on nodes by their key, so websocket with same keys will land on the same machine.
 */
public class HyperIoTWebSocketGroupedBridgeSession extends HyperIoTWebSocketBridgeSession {
    private static Logger log = Logger.getLogger("it.acsoftware.hyperiot");
    private static Map<String, HyperIoTWebSocketGroupedBridgeSession> bridgedSessions;

    static {
        bridgedSessions = Collections.synchronizedMap(new HashMap<String, HyperIoTWebSocketGroupedBridgeSession>());
    }

    /**
     * WebSocket session of the partecipant.
     */
    private HyperIoTWebSocketGroupedBridgeSession bridgedSession;

    public HyperIoTWebSocketGroupedBridgeSession(Session session, boolean authenticated) {
        super(session, authenticated);
    }

    public HyperIoTWebSocketGroupedBridgeSession(Session session, boolean authenticated, HyperIoTWebSocketEncryptionPolicy encryptionPolicy) {
        super(session, authenticated, encryptionPolicy);
    }

    public HyperIoTWebSocketGroupedBridgeSession(Session session, boolean authenticated, HyperIoTWebSocketCompressionPolicy compressionPolicy) {
        super(session, authenticated, compressionPolicy);
    }

    public HyperIoTWebSocketGroupedBridgeSession(Session session, boolean authenticated, HyperIoTWebSocketEncryptionPolicy encryptionPolicy, HyperIoTWebSocketCompressionPolicy compressionPolicy) {
        super(session, authenticated, encryptionPolicy, compressionPolicy);
    }

    /**
     * When the websocket receives the message, it is forwarded to the counterpart (the partecipant) on the bridgedSession.
     *
     * @param message
     */
    @Override
    public void onMessage(String message) {
        if (bridgedSession != null)
            bridgedSession.sendRemote(message);
    }


    @Override
    public void dispose() {
        this.manageClosingBridgedSessions();
        this.bridgedSession = null;
        super.dispose();
    }

    /**
     * Method invoked for adding a partecipant to a existent websocket session
     *
     * @param bridgedSession
     * @param key
     */
    private void addPartecipantSession(HyperIoTWebSocketGroupedBridgeSession bridgedSession, String key) {
        log.log(Level.FINE, "Setting bridged session...");
        //Enforcing key equality, redundant but secure
        this.bridgedSession = bridgedSession;
        if (key.equals(this.getKey())) {
            if (this.bridgedSession != null) {
                if (this.isBridgeOwner()) {
                    HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, " One partecipant....adding...".getBytes(), HyperIoTWebSocketMessageType.PARTECIPANT_ADDED);
                    this.sendRemote(m);
                    //CallBack
                    this.onPartecipantAdded();
                } else {
                    HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, " Connected !".getBytes(), HyperIoTWebSocketMessageType.CONNECTION_OK);
                    this.sendRemote(m);
                    //Callback
                    this.onBridgeOwnerOpenConnection();
                }
            }
        } else {
            HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, "WS socket is about to close, key do not match!".getBytes(), HyperIoTWebSocketMessageType.ERROR);
            this.sendRemote(m);
            this.getSession().close();
            this.onPartecipantClose();
            this.onBridgeOwnerClose();
        }
    }

    private HyperIoTWebSocketGroupedBridgeSession getBridgedSession() {
        return bridgedSession;
    }


    protected void manageOpenPartecipantBridgeSession() {
        boolean closeSession = false;
        String closeMessage = "";
        if (this.getSession().getUpgradeRequest().getParameterMap().containsKey(HyperIoTConstants.HYPERIOT_BRIDGED_WEB_SOCKET_KEY_PARAM)) {
            String key = this.getSession().getUpgradeRequest().getParameterMap().get(HyperIoTConstants.HYPERIOT_BRIDGED_WEB_SOCKET_KEY_PARAM).get(0);
            if (key == null || key.isEmpty())
                closeSession = true;
            else {
                HyperIoTWebSocketGroupedBridgeSession bridgingSession = (HyperIoTWebSocketGroupedBridgeSession) bridgedSessions.get(key);
                if ((bridgingSession == null || bridgingSession.getBridgedSession() != null)) {
                    closeSession = true;
                    closeMessage = "No Session or bridge already setup with another client";
                } else {
                    log.log(Level.FINE, "Adding web socket bridged session on websocket partecipant to bridged session map...");
                    this.setKey(key);
                    //sharing the 2 sessions
                    this.addPartecipantSession(bridgingSession, key);
                    bridgingSession.addPartecipantSession(this, key);
                }
            }
        } else {
            closeSession = true;
        }

        if (closeSession) {
            HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, closeMessage.getBytes(), HyperIoTWebSocketMessageType.ERROR);
            this.sendRemote(m);
            //forcing to close connection
            this.dispose();
            log.log(Level.INFO, "Closing session because: " + closeMessage);
        }
    }

    /**
     * When a bridge web socket is being closed, the counterpart must be closed too
     */
    private void manageClosingBridgedSessions() {
        if (this.isBridgeOwner()) {
            if (this.getBridgedSession() != null) {
                HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, "Bridge owner is closing connection...".getBytes(), HyperIoTWebSocketMessageType.CONNECTION_OWNER_LEAVING);
                this.getBridgedSession().sendRemote(m);
                this.getBridgedSession().dispose();
            }
        } else {
            if (this.getBridgedSession() != null) {
                HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, "partecipant is disconnecting...".getBytes(), HyperIoTWebSocketMessageType.PARTECIPANT_GONE);
                this.getBridgedSession().sendRemote(m);
                this.getBridgedSession().addPartecipantSession(null, this.getKey());
            }
        }
    }

    @Override
    protected void notifyBridgeOwnerKeyAdded(String key) {
        return;
    }

    @Override
    protected void notifyBridgeOwnerKeyDeleted(String key) {
        return;
    }

    @Override
    protected void onBridgeOwnerConnected() {
        bridgedSessions.put(this.getKey(), this);
        return;
    }

    @Override
    protected void onPartecipantClose() {
        return;
    }

    @Override
    protected void onBridgeOwnerClose() {
        bridgedSessions.remove(this.getKey());
        return;
    }

    @Override
    protected void onBridgeOwnerOpenConnection() {
        return;
    }

    @Override
    protected void onPartecipantAdded() {
        return;
    }

    /**
     * @return
     */
    public int countActiveWS() {
        int sessionCount = 0;
        if (this.getSession() != null)
            sessionCount++;
        if (this.bridgedSession != null)
            sessionCount++;
        //are always on the same machine
        return sessionCount;
    }

}
