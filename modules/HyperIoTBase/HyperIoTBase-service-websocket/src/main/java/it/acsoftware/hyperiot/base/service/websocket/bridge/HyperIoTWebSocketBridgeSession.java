package it.acsoftware.hyperiot.base.service.websocket.bridge;

import it.acsoftware.hyperiot.base.api.HyperIoTWebSocketSession;
import it.acsoftware.hyperiot.base.model.HyperIoTWebSocketMessage;
import it.acsoftware.hyperiot.base.model.HyperIoTWebSocketMessageType;
import it.acsoftware.hyperiot.base.service.websocket.HyperIoTWebSocketAbstractSession;
import it.acsoftware.hyperiot.base.service.websocket.WebSocketService;
import it.acsoftware.hyperiot.base.service.websocket.compression.HyperIoTWebSocketCompressionPolicy;
import it.acsoftware.hyperiot.base.service.websocket.encryption.policy.HyperIoTWebSocketEncryptionPolicy;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import org.eclipse.jetty.websocket.api.Session;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Author Aristide Cittadino.
 * Bridged sessions are 2 web socket sessions comunicating each other as a single channel, used in realtime communication
 * between 2 clients that cannot talk each othern directly or in different ways.
 * We define two actors:
 * - The websocket owner: user who opens first the connection and define the key that must be used for other users to connect to the same session
 * - The partecipant: user who wants to partecipate to a websocket session that must specify the same key of the owner who wants to be connected to
 */
public abstract class HyperIoTWebSocketBridgeSession extends HyperIoTWebSocketAbstractSession {
    private static Logger log = Logger.getLogger(HyperIoTWebSocketBridgeSession.class.getName());

    //bridged session list passed to the component from web socket service
    private static Set<String> bridgeUsedKeys = null;

    /**
     * The session key, which identifies users that want to communicated  eachother
     */
    private String key;

    static {
        bridgeUsedKeys = Collections.synchronizedSet(new HashSet<String>());
    }

    /**
     * @param session
     * @param authenticated
     */
    public HyperIoTWebSocketBridgeSession(Session session, boolean authenticated) {
        super(session, authenticated);
    }

    /**
     * @param session
     * @param authenticated
     * @param encryptionPolicy
     */
    public HyperIoTWebSocketBridgeSession(Session session, boolean authenticated, HyperIoTWebSocketEncryptionPolicy encryptionPolicy) {
        super(session, authenticated, encryptionPolicy);
    }

    /**
     * @param session
     * @param authenticated
     * @param compressionPolicy
     */
    public HyperIoTWebSocketBridgeSession(Session session, boolean authenticated, HyperIoTWebSocketCompressionPolicy compressionPolicy) {
        super(session, authenticated, compressionPolicy);
    }

    /**
     * @param session
     * @param authenticated
     * @param encryptionPolicy
     * @param compressionPolicy
     */
    public HyperIoTWebSocketBridgeSession(Session session, boolean authenticated, HyperIoTWebSocketEncryptionPolicy encryptionPolicy, HyperIoTWebSocketCompressionPolicy compressionPolicy) {
        super(session, authenticated, encryptionPolicy, compressionPolicy);
    }

    /**
     * @param key
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * @return
     */
    public String getKey() {
        return key;
    }

    /**
     *
     */
    public void sendBridgeOwnerConnectionMessage() {
        if (this.isBridgeOwner()) {
            HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, "Waiting for other connection".getBytes(), HyperIoTWebSocketMessageType.CONNECTION_OK);
            this.sendRemote(m);
            onBridgeOwnerConnected();
        }
    }

    /**
     * @return
     */
    public boolean isBridgeOwner() {
        return this.getSession().getUpgradeRequest().getParameterMap().containsKey(HyperIoTConstants.HYPERIOT_BRIDGED_WEB_SOCKET_EXPECTED_KEY_PARAM);
    }

    /**
     *
     */
    @Override
    public void initialize() {
        if (!this.getSession().getUpgradeRequest().getParameterMap().containsKey(HyperIoTConstants.HYPERIOT_BRIDGED_WEB_SOCKET_EXPECTED_KEY_PARAM)) {
            manageOpenPartecipantBridgeSession();
        } else {
            manageOpenOwnerBridgeSession();
        }
    }

    /**
     *
     */
    @Override
    public void dispose() {
        if (this.isBridgeOwner()) {
            bridgeUsedKeys.remove(this.getKey());
            this.notifyBridgeOwnerKeyDeleted(this.getKey());
        }
        this.setKey(null);
        HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, "Disconnecting...".getBytes(), HyperIoTWebSocketMessageType.DISCONNECTING);
        this.sendRemote(m);
        this.getMessageBroker().onCloseSession(this.getSession());
        super.dispose();
        if (this.isBridgeOwner())
            this.onBridgeOwnerClose();
        else
            this.onPartecipantClose();
    }


    /**
     * @param key current generated key
     * @return true if the key follows simple rules:
     * 1) Lenght > 10
     * 2) No other sessions should have the current key
     */
    protected boolean isValidBridgeWebSocketKey(String key) {
        // TO DO: should validate more specifically to enforce security
        return key != null && !key.isEmpty() && key.length() >= 10 && !bridgeUsedKeys.contains(key);
    }

    /**
     *
     */
    protected void manageOpenOwnerBridgeSession() {
        boolean closeSession = false;
        String closeMessage = "";
        String key = this.getSession().getUpgradeRequest().getParameterMap().get(HyperIoTConstants.HYPERIOT_BRIDGED_WEB_SOCKET_EXPECTED_KEY_PARAM).get(0);
        if (!isValidBridgeWebSocketKey(key)) {
            closeSession = true;
            closeMessage = "Invalid key (must be at least 10 characters) or key already in use!";
        } else {
            //forcing client which opens the bridge connection to be authenticated
            if (this.isAuthenticated()) {
                //this session is the owner of the bridge
                log.log(Level.FINE, "Adding web socket bridged session on websocket owner to bridged session map...");
                this.setKey(key);
                this.sendBridgeOwnerConnectionMessage();
                bridgeUsedKeys.add(key);
                notifyBridgeOwnerKeyAdded(key);
            } else {
                closeSession = true;
                closeMessage = "Not authenticated,closing session";
            }
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
     * @param key
     */
    protected abstract void notifyBridgeOwnerKeyAdded(String key);

    /**
     * @param key
     */
    protected abstract void notifyBridgeOwnerKeyDeleted(String key);

    /**
     * CALLBACK Methods to override in children
     **/

    protected abstract void onBridgeOwnerConnected();

    /**
     * Callback for closing the partecipant connection
     */
    protected abstract void onPartecipantClose();

    /**
     * Callback for closing the owner connection
     */
    protected abstract void onBridgeOwnerClose();

    /**
     * Callback for Owen opening a connection
     */
    protected abstract void onBridgeOwnerOpenConnection();

    /**
     * Callback for partecipant being added to a session
     */
    protected abstract void onPartecipantAdded();

    /**
     *
     */
    protected abstract void manageOpenPartecipantBridgeSession();

    /**
     *
     */
    protected abstract int countActiveWS();

    public static Set<String> getBridgeUsedKeys() {
        return bridgeUsedKeys;
    }

    /**
     *
     */
    public static void onBridgeOwnerKeyAdded(String key) {
        bridgeUsedKeys.add(key);
    }

    /**
     *
     */
    public static void onBridgeOwnerKeyADeleted(String key) {
        bridgeUsedKeys.remove(key);
    }

    /**
     * @return the current number of open bridged websocket
     */
    public static int getWebSocketBridgeCount() {
        Iterator<Session> it = WebSocketService.getSessions().keySet().iterator();

        int onThisServerCount = 0;
        HashSet<String> keys = new HashSet<>();
        while (it.hasNext()) {
            Session session = it.next();
            HyperIoTWebSocketSession hytSession = WebSocketService.getSessions().get(session);
            if (hytSession instanceof HyperIoTWebSocketBridgeSession) {
                HyperIoTWebSocketBridgeSession b = (HyperIoTWebSocketBridgeSession) hytSession;
                //in clustered environment number of ws must be processed exactly once
                log.log(Level.FINE, "Checking ws count for key: {0}", new Object[]{b.getKey()});
                onThisServerCount++;
            }
        }
        return onThisServerCount;
    }

}
