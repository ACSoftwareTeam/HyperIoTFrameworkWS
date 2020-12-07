package it.acsoftware.hyperiot.kafka.connector.service.websocket;

import it.acsoftware.hyperiot.base.model.HyperIoTWebSocketMessage;
import it.acsoftware.hyperiot.base.model.HyperIoTWebSocketMessageType;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.service.websocket.bridge.HyperIoTWebSocketClusteredBridgeSession;
import it.acsoftware.hyperiot.base.service.websocket.compression.HyperIoTWebSocketCompressionPolicy;
import it.acsoftware.hyperiot.base.service.websocket.encryption.mode.HyperIoTRSAWithAESEncryptionMode;
import it.acsoftware.hyperiot.base.service.websocket.encryption.policy.HyperIoTWebSocketEncryptionPolicy;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaConnectorSystemApi;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaMessageReceiver;
import it.acsoftware.hyperiot.kafka.connector.model.HyperIoTKafkaMessage;
import it.acsoftware.hyperiot.kafka.connector.util.HyperIoTKafkaConnectorConstants;
import org.eclipse.jetty.websocket.api.Session;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import reactor.core.Disposable;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @Author Aristide Cittadino
 * This component uses the standard WebSocketBridgetSession to implement web socket bridge via kafka.
 * The main difference between this and its parent is related to performance.
 * This component should be used wether the communication between the 2 web socket is intensive, so for high load.
 * The standard web socket bridge doesn't perform well in presence of high load within time limit in communication (for example each message must be delivered in 2 secs).
 * For this cases please use the KafkaWebSocketBridgeSession
 */
public abstract class KafkaWebSocketBridgeSession extends HyperIoTWebSocketClusteredBridgeSession implements KafkaMessageReceiver {
    private static Logger logger = Logger.getLogger(KafkaWebSocketBridgeSession.class.getName());

    //Same Kafka topic but different partitions:
    // Partition 1 used from the bridge owner
    // Partition 2 used from the bridge guest
    public static final String BRIDGE_OWNER_KEY_SUFFIX = "owner";
    public static final String BRIDGE_PARTECIPANT_KEY_SUFFIX = "partecipant";

    public static final String KAFKA_KEY_SEPARATOR = ":";
    public static final String WS_AES_DATA_SEPARATOR = ":";
    /**
     * When the component receive the message from the other side of the websocket
     * it forwards the message to kafka on specific topic in the owner/guest partition.
     * This topic is the one which the other part of the websocket is attached to.
     */
    private volatile String bridgeKafkaTopic;

    /**
     * sessionStartTimestamp for identify uniquely a bridge session <timestamp><key>
     */
    private String keyPrefix;
    /**
     * Key that must be matched for receiving messages.
     * This is used since there's one topic and multiple partitions
     */
    private String keyRead;

    /**
     * Key that must me put in the message for writing
     */
    private String keyWrite;

    /**
     * Boolean for activating or deactivating encryted messages on websocket
     */
    private boolean messageEncryption;

    /**
     * KafkaConnector
     */
    private KafkaConnectorSystemApi kafkaConnectorSystemApi;

    /**
     *
     */
    private String partecipantRequestNotificationToBridgeOwnerKafkaKey;
    /**
     *
     */
    private String ownerFoundNotificationToPartecipantKafkaKey;
    /**
     *
     */
    private String partecipantAddedNotificationToBridgeOwnerKafkaKey;
    /**
     *
     */
    private String ownerDisconnectedNotificationToPartecipantKafkaKey;
    /**
     *
     */
    private String partecipantDisconnectedNotificationToBridgeOwnerKafkaKey;
    /**
     *
     */
    private String bridgeKeyAddedKafkaKey;
    /**
     *
     */
    private String bridgeKeyRemovedKafkaKey;

    /**
     *
     */
    private ServiceReference reference;

    private ServiceRegistration registration;

    /**
     * Flux for Kafka communication
     */
    private Disposable fluxSubscription;

    /**
     * @param session
     * @param authenticated
     */
    public KafkaWebSocketBridgeSession(Session session, boolean authenticated) {
        super(session, authenticated);
        //Kafka Web Socket bridge session allows partecipant to be on different servers, so the bridge is clustered
        this.messageEncryption = false;
        init();
    }

    /**
     * @param session
     * @param authenticated
     * @param messageEncryption
     */
    public KafkaWebSocketBridgeSession(Session session, boolean authenticated, boolean messageEncryption) {
        super(session, authenticated);
        //Kafka Web Socket bridge session allows partecipant to be on different servers, so the bridge is clustered
        this.messageEncryption = messageEncryption;
        init();
    }

    /**
     * @param session
     * @param authenticated
     * @param encryptionPolicy
     * @param messageEncryption
     */
    public KafkaWebSocketBridgeSession(Session session, boolean authenticated, HyperIoTWebSocketEncryptionPolicy encryptionPolicy, boolean messageEncryption) {
        super(session, authenticated, encryptionPolicy);
        this.messageEncryption = messageEncryption;
        init();
    }

    /**
     * @param session
     * @param authenticated
     * @param compressionPolicy
     * @param messageEncryption
     */
    public KafkaWebSocketBridgeSession(Session session, boolean authenticated, HyperIoTWebSocketCompressionPolicy compressionPolicy, boolean messageEncryption) {
        super(session, authenticated, compressionPolicy);
        this.messageEncryption = messageEncryption;
        init();
    }

    /**
     * @param session
     * @param authenticated
     * @param encryptionPolicy
     * @param compressionPolicy
     * @param messageEncryption
     */
    public KafkaWebSocketBridgeSession(Session session, boolean authenticated, HyperIoTWebSocketEncryptionPolicy encryptionPolicy, HyperIoTWebSocketCompressionPolicy compressionPolicy, boolean messageEncryption) {
        super(session, authenticated, encryptionPolicy, compressionPolicy);
        this.messageEncryption = messageEncryption;
        init();
    }

    /**
     * @param key
     */
    @Override
    public void setKey(String key) {
        if (key != null) {
            super.setKey(key);
            this.keyRead = this.getKey() + "-" + (this.isBridgeOwner() ? BRIDGE_OWNER_KEY_SUFFIX : BRIDGE_PARTECIPANT_KEY_SUFFIX);
            this.keyWrite = this.getKey() + "-" + (this.isBridgeOwner() ? BRIDGE_PARTECIPANT_KEY_SUFFIX : BRIDGE_OWNER_KEY_SUFFIX);
            //when the key is set the component is registered to OSGi
            this.registerOSGiForKafkaConsuming();
        }
    }


    /**
     * When a message on websocket arrives it's forwarded to Kafka.
     * If encryption is enabled, the message is decrypted in order to let plain text analysis on Kafka.
     *
     * @param message
     */
    @Override
    public void onMessage(String message) {
        //bridgeKafkaTopic on partecipant can be null if no confirm has been received from the owner
        logger.log(Level.FINE, "Received message {0}, forwarding to {1} and to key {2} ", new Object[]{message, this.bridgeKafkaTopic, this.keyWrite});
        long onMessageInitialTime = System.nanoTime();
        if (this.bridgeKafkaTopic != null) {
            String kafkaKey = this.getKafkaMessageKey();
            //Parsing message to identiy if it must be processed on server side
            // TO DO: fast identification on messages which must be processed on server side
            // we should avoid jackson deserialization
            byte[] rawMessage = this.getMessageBroker().readRaw(message);
            HyperIoTWebSocketMessage wsMessage = HyperIoTWebSocketMessage.fromString(new String(rawMessage));
            if (wsMessage != null && wsMessage.getType().equals(HyperIoTWebSocketMessageType.PROCESS_ON_SERVER)) {
                //message must be processed on server
                try {
                    String serverResponse = processMessageOnServer(wsMessage);
                    //getting server response, send back to the client
                    if (serverResponse != null) {
                        HyperIoTWebSocketMessage responseMessage = HyperIoTWebSocketMessage.createMessage(null, serverResponse.getBytes("UTF8"), HyperIoTWebSocketMessageType.PROCESS_ON_SERVER_RESULT);
                        this.sendRemote(responseMessage);
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    //sending back to the client the error message
                    HyperIoTWebSocketMessage errMessage = HyperIoTWebSocketMessage.createMessage(null, e.getMessage().getBytes(), HyperIoTWebSocketMessageType.ERROR);
                    this.sendRemote(errMessage);
                }
            } else {
                //If message doesn't have to be processed on server side, it is forwarded to kafka
                HyperIoTKafkaMessage kMessage = new HyperIoTKafkaMessage(this.keyWrite.getBytes(), this.bridgeKafkaTopic, rawMessage);
                this.kafkaConnectorSystemApi.produceMessage(kMessage);
            }
        }
        if (logger.isLoggable(Level.FINER)) {
            long onMessageEndTime = System.nanoTime();
            double duration = (double) (onMessageEndTime - onMessageInitialTime) / 1_000_000;
            logger.log(Level.FINE, "On Message websocket time is: {0}", duration);
        }
    }

    /**
     * method invoked when a message from kafka arrives on system topic.
     * This method is used to orchestrate the connection between clients and share communication infos
     *
     * @param message
     */
    @Override
    public void receive(HyperIoTKafkaMessage message) {
        if (message.getTopic().equalsIgnoreCase(this.kafkaConnectorSystemApi.getClusterSystemTopic())) {
            String kafkaKey = new String(message.getKey());
            logger.log(Level.FINE, "Received message from kafka web socket with kafka key {0} ws key {1}, owner: {2}", new Object[]{kafkaKey, this.getKey(), this.isBridgeOwner()});

            int nodeIdIndex = kafkaKey.indexOf(KAFKA_KEY_SEPARATOR);
            if (nodeIdIndex < 0) {
                logger.log(Level.SEVERE, "Error, NO separator found for Kafka Message!");
                return;
            }

            String[] tokens = kafkaKey.split(KAFKA_KEY_SEPARATOR);
            if (tokens.length < 2) {
                logger.log(Level.SEVERE, "Error, message received but wrong number of arguments inside message!");
                return;
            }

            String keyPayload = tokens[0];
            String webSocketSessionKey = tokens[1];


            //checking node id is present and the message is related
            logger.log(Level.FINE, "Received message with key {0} compared to key {1}", new String[]{tokens[1], this.getKey()});
            if (kafkaKey != null && kafkaKey.startsWith(HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_KAFKA_KEY_PREFIX) && webSocketSessionKey != null && webSocketSessionKey.equalsIgnoreCase(this.getKey())) {
                String kafkaBodyPayload = new String(message.getPayload());
                if (keyPayload.equalsIgnoreCase(HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_PARTECIPAND_REQUEST)) {
                    //if i am the owner some partecipant wants to join must check my key
                    if (this.isBridgeOwner() && getBridgeUsedKeys().contains(webSocketSessionKey)) {
                        //Key found, notify the partecipant, still not sending message to WS waiting for partecipant confirmation
                        //configuring data communication on the owner, keyPrefix null means it will be generated
                        configureCommunication(this.getKafkaTopic(), null);
                        this.registerForKafkaMessage();
                        String data = this.getKafkaTopic() + KAFKA_KEY_SEPARATOR + this.keyPrefix;
                        if (this.messageEncryption) {
                            //If encryption enabled generating AES key
                            if (messageEncryption) {
                                try {
                                    Map<String, Object> aesParams = this.getMessageBroker().getEncryptionPolicyParams();
                                    String aesPwdBase64 = new String(Base64.getEncoder().encode((byte[]) aesParams.get(HyperIoTRSAWithAESEncryptionMode.MODE_PARAM_AES_PASSWORD)));
                                    String aesIvBase64 = new String(Base64.getEncoder().encode((byte[]) aesParams.get(HyperIoTRSAWithAESEncryptionMode.MODE_PARAM_AES_IV)));
                                    String payload = aesPwdBase64 + WS_AES_DATA_SEPARATOR + aesIvBase64;
                                    data += KAFKA_KEY_SEPARATOR + aesPwdBase64 + KAFKA_KEY_SEPARATOR + aesIvBase64;
                                    this.notifyOwnerToBridgePartecipant(data.getBytes());
                                } catch (Exception e) {
                                    logger.log(Level.SEVERE, e.getMessage(), e);
                                }
                            } else {
                                this.notifyOwnerToBridgePartecipant(data.getBytes());
                            }
                        }

                    }
                } else if (keyPayload.equalsIgnoreCase(HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_OWNER_KEY_FOUND)) {
                    //if i am the guest the owner is sending his data, and the key was correct
                    //get kafka topic
                    if (!this.isBridgeOwner()) {
                        String topicAndKey = kafkaBodyPayload;
                        String[] dataSplit = topicAndKey.split(KAFKA_KEY_SEPARATOR);
                        configureCommunication(dataSplit[0], dataSplit[1]);
                        HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, " Connected !".getBytes(), HyperIoTWebSocketMessageType.CONNECTION_OK);
                        //sending ok to connection
                        this.sendRemote(m);
                        if (messageEncryption) {
                            //must receive even key! for AES encryption
                            if (dataSplit.length == 4) {
                                try {
                                    String aesPwdStrEncoded = dataSplit[2];
                                    String ivEncoded = dataSplit[3];
                                    byte[] aesPwdDecoded = Base64.getDecoder().decode(aesPwdStrEncoded.getBytes("UTF8"));
                                    byte[] aesIVDecoded = Base64.getDecoder().decode(ivEncoded.getBytes("UTF8"));
                                    String payload = aesPwdStrEncoded + WS_AES_DATA_SEPARATOR + ivEncoded;
                                    m = HyperIoTWebSocketMessage.createMessage(null, payload.getBytes("UTF8"), HyperIoTWebSocketMessageType.SET_ENCRYPTION_KEY);
                                    //sending to reset key
                                    this.sendRemote(m);
                                    KafkaWebSocketBridgeSession.this.updatePolicyParams(aesPwdDecoded, aesIVDecoded);
                                } catch (Exception e) {
                                    logger.log(Level.SEVERE, e.getMessage(), e);
                                }
                            }
                        }
                        this.registerForKafkaMessage();
                        this.notifyPartecipantAddedToBridgeOwner();
                    }
                } else if (keyPayload.equalsIgnoreCase(HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_PARTECIPAND_ADDED)) {
                    //if i am the owner some partecipant want to join must check my key
                    if (this.isBridgeOwner()) {
                        HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, " One partecipant....adding...".getBytes(), HyperIoTWebSocketMessageType.PARTECIPANT_ADDED);
                        this.sendRemote(m);
                        this.onPartecipantAdded();
                    }
                } else if (keyPayload.equalsIgnoreCase(HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_OWNER_DISCONNECTED)) {
                    //if i am the guest the owner is disconnecting
                    if (!this.isBridgeOwner()) {
                        HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, "Bridge owner is closing connection...".getBytes(), HyperIoTWebSocketMessageType.CONNECTION_OWNER_LEAVING);
                        this.sendRemote(m);
                        this.dispose();
                    }
                } else if (keyPayload.equalsIgnoreCase(HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_PARTECIPAND_DISCONNECTED)) {
                    //if i am the owner the partecipant is disconnecting
                    if (this.isBridgeOwner()) {
                        HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, "partecipant is disconnecting...".getBytes(), HyperIoTWebSocketMessageType.PARTECIPANT_GONE);
                        this.sendRemote(m);
                    }
                }
            } else {
                logger.log(Level.WARNING, "Wrong OSGi filter this Component should not be invoked from kafka receiver for this topic and key");
            }
        } else if (message.getTopic().equalsIgnoreCase(this.bridgeKafkaTopic)) {
            String kafkaKey = new String(message.getKey());
            logger.log(Level.FINE, "One message received on topic {0} with key {1} current key is {2}, checking if should send back to WS...", new Object[]{this.bridgeKafkaTopic, new String(message.getKey()), this.getKafkaMessageKey()});
            //avoiding sharing same topic and same partition but different keys on communication
            if (kafkaKey.equals(this.keyRead)) {
                logger.log(Level.FINE, "correct key found, sending...");
                byte[] messageToSend = message.getPayload();
                //Sending message to the counterpart
                this.getMessageBroker().sendAsync(new String(messageToSend));
            }

        }
    }

    /**
     * The error is forwarded to the websocket session
     *
     * @param var1
     */
    public void onError(Throwable var1) {
        HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage((String) null, ("Error while receiving data" + var1.getCause()).getBytes(), HyperIoTWebSocketMessageType.ERROR);
        this.sendRemote(m);
    }


    /**
     * Method used for registering to kafka and notifify the other part of the websocket
     */
    protected void registerForKafkaMessage() {
        List<String> communicationTopics = new ArrayList<>();
        communicationTopics.add(this.bridgeKafkaTopic);
        List<String> communicationKeys = new ArrayList<>();
        communicationKeys.add(this.getKafkaMessageKey());
        //Adding specific topic and partition filter in order to receive messages only for that topic and that partition
        Dictionary dict = this.getServiceRegistrationProperties(communicationTopics, communicationKeys);
        //adding partition info
        this.registration.setProperties(dict);
    }

    /**
     *
     */
    @Override
    public void dispose() {
        this.unregisterOSGiForKafkaConsuming();
        try {
            if (fluxSubscription != null)
                this.fluxSubscription.dispose();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        super.dispose();
    }

    /**
     * Can be overridden so user can specify custom topics, default is websocket key
     *
     * @return
     */
    protected String getKafkaTopic() {
        //default returnin bridge key
        return getKey();
    }

    /**
     * Can be overridden so user can specify custom topics, default is websocket key
     *
     * @return kafka message key
     */
    protected String getKafkaMessageKey() {
        //default returning websocker bridge key
        return this.keyPrefix + "-" + this.getKey();
    }

    /**
     * Default every message is not processed on server.
     * This method can be overridden in order to make some server logic on server which is not propagated to the other endpoint of the
     * websocket
     *
     * @param message kafka message receved
     * @return
     */
    protected String processMessageOnServer(HyperIoTWebSocketMessage message) {
        return null;
    }

    /**
     * @param topic
     * @param keyPrefix
     */
    protected void configureCommunication(String topic, String keyPrefix) {
        if (keyPrefix == null) {
            Date now = new Date();
            String userIdentifier = this.getUserIdentifier();
            //adding user information inside key
            if (userIdentifier != null && userIdentifier.length() > 0) {
                //if user identifiers uses forbidden characters we substituite with "_"
                this.keyPrefix = this.getUserIdentifier().replaceAll(KAFKA_KEY_SEPARATOR, "_").replaceAll(WS_AES_DATA_SEPARATOR, "_") + "-" + String.valueOf(now.getTime());
            } else
                this.keyPrefix = String.valueOf(now.getTime());
        } else {
            this.keyPrefix = keyPrefix;
        }
        this.bridgeKafkaTopic = topic;
    }

    /**
     * Callback to overide
     */
    protected void onKafkaMessageError(Throwable error) {
        return;
    }


    /**
     * Notify that a partecipant is connected to the owner (if it is present)
     */
    @Override
    protected void notifyPartecipantRequestToBridgeOwner() {
        //sending message with empty body
        HyperIoTKafkaMessage kMessage = new HyperIoTKafkaMessage(partecipantRequestNotificationToBridgeOwnerKafkaKey.getBytes(), this.kafkaConnectorSystemApi.getClusterSystemTopic(), "".getBytes());
        this.kafkaConnectorSystemApi.produceMessage(kMessage);
    }

    /**
     * Notify to the partecipant that an owner has been found
     *
     * @param data data to share
     */
    @Override
    protected void notifyOwnerToBridgePartecipant(byte[] data) {
        HyperIoTKafkaMessage kMessage = new HyperIoTKafkaMessage(ownerFoundNotificationToPartecipantKafkaKey.getBytes(), this.kafkaConnectorSystemApi.getClusterSystemTopic(), data);
        this.kafkaConnectorSystemApi.produceMessage(kMessage);
    }

    /**
     * Notify the bridge owner that partecipant is connected
     */
    @Override
    protected void notifyPartecipantAddedToBridgeOwner() {
        HyperIoTKafkaMessage kMessage = new HyperIoTKafkaMessage(partecipantAddedNotificationToBridgeOwnerKafkaKey.getBytes(), this.kafkaConnectorSystemApi.getClusterSystemTopic(), "".getBytes());
        this.kafkaConnectorSystemApi.produceMessage(kMessage);
    }

    /**
     * Notify Owner disconnection to the partecipant
     */
    @Override
    protected void notifyOwnerDisconnectedToBridgePartecipant() {
        HyperIoTKafkaMessage kMessage = new HyperIoTKafkaMessage(ownerDisconnectedNotificationToPartecipantKafkaKey.getBytes(), this.kafkaConnectorSystemApi.getClusterSystemTopic(), "".getBytes());
        this.kafkaConnectorSystemApi.produceMessage(kMessage);
    }

    /**
     * Notify Partecipant disconnection to the owner
     */
    @Override
    protected void notifyPartecipantDisconnectedToBridgeOwner() {
        HyperIoTKafkaMessage kMessage = new HyperIoTKafkaMessage(partecipantDisconnectedNotificationToBridgeOwnerKafkaKey.getBytes(), this.kafkaConnectorSystemApi.getClusterSystemTopic(), "".getBytes());
        this.kafkaConnectorSystemApi.produceMessage(kMessage);
    }

    @Override
    protected void notifyBridgeOwnerKeyAdded(String s) {
        HyperIoTKafkaMessage kMessage = new HyperIoTKafkaMessage(bridgeKeyAddedKafkaKey.getBytes(), this.kafkaConnectorSystemApi.getClusterSystemTopic(), s.getBytes());
        this.kafkaConnectorSystemApi.produceMessage(kMessage);
    }

    @Override
    protected void notifyBridgeOwnerKeyDeleted(String s) {
        HyperIoTKafkaMessage kMessage = new HyperIoTKafkaMessage(bridgeKeyRemovedKafkaKey.getBytes(), this.kafkaConnectorSystemApi.getClusterSystemTopic(), s.getBytes());
        this.kafkaConnectorSystemApi.produceMessage(kMessage);
    }

    /**
     * In cluster mode the topic for the partecipant is sent by the owner.
     * So if the topic is set then a notification has been received.
     *
     * @return
     */
    @Override
    protected boolean checkOwnerNotificationReceived() {
        if (!this.isBridgeOwner()) {
            return this.bridgeKafkaTopic != null;
        }
        return false;
    }

    /**
     *
     */
    @Override
    protected void onBridgeOwnerConnected() {
        //generating private key and send back to the client
        if (messageEncryption) {
            try {
                byte[] aesPwd = HyperIoTSecurityUtil.generateRandomAESPassword();
                byte[] iv = HyperIoTSecurityUtil.generateRandomAESInitVector();
                //sending <password:iv>
                String aesPwdBase64 = new String(Base64.getEncoder().encode(aesPwd));
                String aesIvBase64 = new String(Base64.getEncoder().encode(iv));
                String payload = aesPwdBase64 + WS_AES_DATA_SEPARATOR + aesIvBase64;
                // no need to encode in base64, it is done by jackson
                HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, payload.getBytes("UTF8"), HyperIoTWebSocketMessageType.SET_ENCRYPTION_KEY);
                this.sendRemote(m);
                KafkaWebSocketBridgeSession.this.updatePolicyParams(aesPwd, iv);
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onBridgeOwnerOpenConnection() {
        return;
    }

    /**
     * @return the user identifier which is part of the kafka key
     */
    protected abstract String getUserIdentifier();


    /**
     * @param aesPwd
     * @param iv
     */
    private void updatePolicyParams(byte[] aesPwd, byte[] iv) {
        Map<String, Object> params = new HashMap<>();
        params.put(HyperIoTRSAWithAESEncryptionMode.MODE_PARAM_AES_PASSWORD, aesPwd);
        params.put(HyperIoTRSAWithAESEncryptionMode.MODE_PARAM_AES_IV, iv);
        KafkaWebSocketBridgeSession.this.getMessageBroker().updateEncryptionPolicyParams(params);
    }

    /**
     *
     */
    private void init() {
        ServiceReference serviceReference = this.getBundleContext().getServiceReference(KafkaConnectorSystemApi.class);
        this.kafkaConnectorSystemApi = (KafkaConnectorSystemApi) this.getBundleContext().getService(serviceReference);
    }


    /**
     *
     */
    private void registerOSGiForKafkaConsuming() {
        logger.log(Level.FINE, "Registering web socket to OSGi services...");
        //at the startup time the component will register only to system topics
        Dictionary dictionary = getServiceRegistrationProperties(null, null);
        BundleContext ctx = HyperIoTUtil.getBundleContext(KafkaWebSocketBridgeSession.class);
        registration = ctx.registerService(KafkaMessageReceiver.class, this, dictionary);
        reference = registration.getReference();

    }

    /**
     * @param topicList
     * @param keyList
     * @return
     */
    private Dictionary getServiceRegistrationProperties(List<String> topicList, List<String> keyList) {
        this.partecipantRequestNotificationToBridgeOwnerKafkaKey = HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_PARTECIPAND_REQUEST + KAFKA_KEY_SEPARATOR + this.getKey();
        this.ownerFoundNotificationToPartecipantKafkaKey = HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_OWNER_KEY_FOUND + KAFKA_KEY_SEPARATOR + this.getKey();
        this.partecipantAddedNotificationToBridgeOwnerKafkaKey = HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_PARTECIPAND_ADDED + KAFKA_KEY_SEPARATOR + this.getKey();
        this.ownerDisconnectedNotificationToPartecipantKafkaKey = HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_OWNER_DISCONNECTED + KAFKA_KEY_SEPARATOR + this.getKey();
        this.partecipantDisconnectedNotificationToBridgeOwnerKafkaKey = HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_PARTECIPAND_DISCONNECTED + KAFKA_KEY_SEPARATOR + this.getKey();
        this.bridgeKeyAddedKafkaKey = HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_NEW_KEY_ADDED;
        this.bridgeKeyRemovedKafkaKey = HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_KEY_DELETED;
        //registering to system topics manually because this instance is create by websocket management
        Hashtable dictionary = new Hashtable<>();
        List<String> basicTopics = new ArrayList<>();
        List<String> basicKeys = new ArrayList<>();
        basicTopics.add(this.kafkaConnectorSystemApi.getClusterSystemTopic());
        if (topicList != null && topicList.size() > 0) {
            basicTopics.addAll(topicList);
        }
        dictionary.put(HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_OSGI_TOPIC_FILTER, basicTopics.toArray());
        //registering only for being notificated on relevant data on same system topic
        if (this.isBridgeOwner()) {
            logger.log(Level.FINE, "Registering owner for key {0} to \n{1}\n{2}\n{3}", new Object[]{partecipantRequestNotificationToBridgeOwnerKafkaKey, partecipantAddedNotificationToBridgeOwnerKafkaKey, partecipantDisconnectedNotificationToBridgeOwnerKafkaKey});
            basicKeys.add(partecipantRequestNotificationToBridgeOwnerKafkaKey);
            basicKeys.add(partecipantAddedNotificationToBridgeOwnerKafkaKey);
            basicKeys.add(partecipantDisconnectedNotificationToBridgeOwnerKafkaKey);
        } else {
            logger.log(Level.FINE, "Registering partecipant for key {0} to \n{1}\n{2}", new Object[]{ownerFoundNotificationToPartecipantKafkaKey, ownerDisconnectedNotificationToPartecipantKafkaKey});
            basicKeys.add(ownerFoundNotificationToPartecipantKafkaKey);
            basicKeys.add(ownerDisconnectedNotificationToPartecipantKafkaKey);
        }

        //registering for receiving from specific key
        basicKeys.add(this.keyRead);

        if (keyList != null && keyList.size() > 0) {
            basicKeys.addAll(keyList);
        }

        dictionary.put(HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_OSGI_KEY_FILTER, basicKeys.toArray());
        return dictionary;
    }

    /**
     *
     */
    private void unregisterOSGiForKafkaConsuming() {
        try {
            registration.unregister();
        } catch (Exception e) {
            logger.log(Level.FINE, e.getMessage(), e);
        }
    }

}
