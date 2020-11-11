package it.acsoftware.hyperiot.base.service.websocket.bridge;

import it.acsoftware.hyperiot.base.model.HyperIoTWebSocketMessage;
import it.acsoftware.hyperiot.base.model.HyperIoTWebSocketMessageType;
import it.acsoftware.hyperiot.base.service.websocket.compression.HyperIoTWebSocketCompressionPolicy;
import it.acsoftware.hyperiot.base.service.websocket.encryption.policy.HyperIoTWebSocketEncryptionPolicy;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import org.eclipse.jetty.websocket.api.Session;

import java.util.concurrent.Executor;
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
 * Clustered Bridge Session are session between 2 clients which are managed in a clustered environment.
 * So the bridge is phisically divided on 2 or more server. Some orchestration is required
 */
public abstract class HyperIoTWebSocketClusteredBridgeSession extends HyperIoTWebSocketBridgeSession {
    private static Logger log = Logger.getLogger("it.acsoftware.hyperiot");

    private Executor executor;
    private boolean partecipantAdded;

    public HyperIoTWebSocketClusteredBridgeSession(Session session, boolean authenticated) {
        super(session, authenticated);
    }

    public HyperIoTWebSocketClusteredBridgeSession(Session session, boolean authenticated, HyperIoTWebSocketEncryptionPolicy encryptionPolicy) {
        super(session, authenticated, encryptionPolicy);
    }

    public HyperIoTWebSocketClusteredBridgeSession(Session session, boolean authenticated, HyperIoTWebSocketCompressionPolicy compressionPolicy) {
        super(session, authenticated, compressionPolicy);
    }

    public HyperIoTWebSocketClusteredBridgeSession(Session session, boolean authenticated, HyperIoTWebSocketEncryptionPolicy encryptionPolicy, HyperIoTWebSocketCompressionPolicy compressionPolicy) {
        super(session, authenticated, encryptionPolicy, compressionPolicy);
    }

    /**
     * Managing partecipant session.
     * Owner session is managed in the parent class.
     */
    protected void manageOpenPartecipantBridgeSession() {
        boolean closeSession = false;
        String closeMessage = "Invalid or missing ws bridge key, or not authenticated!";
        if (this.getSession().getUpgradeRequest().getParameterMap().containsKey(HyperIoTConstants.HYPERIOT_BRIDGED_WEB_SOCKET_KEY_PARAM)) {
            String key = this.getSession().getUpgradeRequest().getParameterMap().get(HyperIoTConstants.HYPERIOT_BRIDGED_WEB_SOCKET_KEY_PARAM).get(0);
            if (key == null || key.isEmpty()) {
                closeSession = true;
            } else {
                log.log(Level.FINE, "Adding web socket bridged session on websocket partecipant to bridged session map...");
                this.setKey(key);
                //must notify the owner in the cluster
                notifyPartecipantRequestToBridgeOwner();
                //start count down on this bridgeSession for receiving ok from the owner
                startCountdownForBridgeOwnerNotification();
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
     * Method used only in clustered sessions.
     * Used for waiting the notitification by the partecipant.
     * If the notification is not received in 30secs then websocket is closed.
     */
    private void startCountdownForBridgeOwnerNotification() {
        if (!this.isBridgeOwner()) {
            log.log(Level.FINE, "Starting timer for websocket partecipant for receivng confirm from the owner.");
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        log.log(Level.SEVERE, e.getMessage(), e);
                    } finally {
                        log.log(Level.FINE, "Web Socket timer finished checking confirmation received...");
                        if (!HyperIoTWebSocketClusteredBridgeSession.this.checkOwnerNotificationReceived()) {
                            log.log(Level.FINE, "Bridge owner confirmation not received, closing websocket...");
                            getBridgeUsedKeys().remove(getKey());
                            HyperIoTWebSocketClusteredBridgeSession.this.notifyBridgeOwnerKeyDeleted(getKey());
                            String closeMessage = "Invalid or missing ws bridge key, or not authenticated!";
                            HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, closeMessage.getBytes(), HyperIoTWebSocketMessageType.ERROR);
                            HyperIoTWebSocketClusteredBridgeSession.this.sendRemote(m);
                            //forcing to close connection
                            HyperIoTWebSocketClusteredBridgeSession.this.dispose();
                        }
                    }
                }
            };
            Thread t = new Thread(r);
            t.start();
        }
    }


    @Override
    protected void onPartecipantClose() {
        this.notifyPartecipantDisconnectedToBridgeOwner();
    }

    @Override
    protected void onBridgeOwnerClose() {
        this.notifyOwnerDisconnectedToBridgePartecipant();
    }

    @Override
    protected void onPartecipantAdded() {
        this.partecipantAdded = true;
    }

    /**
     * if Partecipant on the bridge wants to join the session then a notification to the owner must be sent in order to check the correctness of the key.
     */
    protected abstract void notifyPartecipantRequestToBridgeOwner();

    /**
     * if WebSocket bridge session is created then and it is clustered, then a cluster notification is sent.
     */
    protected abstract void notifyPartecipantAddedToBridgeOwner();

    /**
     * if WebSocket bridge session is created then and it is clustered, then a cluster notification is sent.
     *
     * @param data to share with the counterpart
     */
    protected abstract void notifyOwnerToBridgePartecipant(byte[] data);

    /**
     * Notify to partecipant that owner is disconnected only in clustered mode
     */
    protected abstract void notifyOwnerDisconnectedToBridgePartecipant();

    /**
     * Notify to the owner that the partecipant is disconnected only in clustered mode
     */
    protected abstract void notifyPartecipantDisconnectedToBridgeOwner();

    /**
     * This method is called when cluster bridge session timer expires.
     * At the time of expiration this method checks if the owner notification has been received from the guest or not.
     * If not, the connection is closed.
     */
    protected abstract boolean checkOwnerNotificationReceived();

    public int countActiveWS() {
        int sessionCount = 0;

        if (this.getSession() != null) {
            sessionCount++;
        }

        if (this.isBridgeOwner() && partecipantAdded) {
            sessionCount++;
        } else if (!this.isBridgeOwner() && checkOwnerNotificationReceived()) {
            sessionCount++;
        }

        return sessionCount;
    }

}
