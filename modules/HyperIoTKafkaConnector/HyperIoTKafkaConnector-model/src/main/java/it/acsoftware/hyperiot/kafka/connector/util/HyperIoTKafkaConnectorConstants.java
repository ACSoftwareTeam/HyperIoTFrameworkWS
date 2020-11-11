package it.acsoftware.hyperiot.kafka.connector.util;

/**
 * @author Aristide Cittadino
 * Constants class
 */
public class HyperIoTKafkaConnectorConstants {
    /**
     * Property prefix in the file it.acsoftware.hyperiot.cfg for Kafka consumer properties
     */
    public static final String HYPERIOT_KAFKA_PROP_MAX_CONSUMER_THREADS = "it.acsoftware.hyperiot.kafka.max.consumer.thread";

    /**
     * Property prefix in the file it.acsoftware.hyperiot.cfg for Kafka Reactor consumer threads
     */
    public static final String HYPERIOT_KAFKA_REACTOR_PROP_MAX_CONSUMER_THREADS = "it.acsoftware.hyperiot.kafka.reactor.max.consumer.thread";
    /**
     * Property prefix in the file it.acsoftware.hyperiot.cfg for Kafka consumer properties
     */
    public static final String HYPERIOT_KAFKA_PROPS_CONSUMER_PREFIX = "it.acsoftware.hyperiot.kafka.consumer";
    /**
     * Property prefix in the file it.acsoftware.hyperiot.cfg for Kafka producer properties
     */
    public static final String HYPERIOT_KAFKA_PROPS_PRODUCER_PREFIX = "it.acsoftware.hyperiot.kafka.producer";
    /**
     * Property prefix in the file it.acsoftware.hyperiot.cfg for Kafka admin properties
     */
    public static final String HYPERIOT_KAFKA_PROPS_ADMIN_PREFIX = "it.acsoftware.hyperiot.kafka.admin";
    /**
     * Property prefix in the file it.acsoftware.hyperiot.cfg for Kafla consumer,producer and admin properties
     */
    public static final String HYPERIOT_KAFKA_PROPS_GLOBAL_PREFIX = "it.acsoftware.hyperiot.kafka.all";
    /**
     * Property used inside OSGi component registration/loading to filter components for specifics topics
     */
    public static final String HYPERIOT_KAFKA_OSGI_TOPIC_FILTER = "it.acsoftware.hyperiot.kafka.topic";

    /**
     * Property used inside OSGi component registration/loading to filter components for specifics topics
     */
    public static final String HYPERIOT_KAFKA_OSGI_KEY_FILTER = "it.acsoftware.hyperiot.kafka.key";

    /**
     * Basic Kafka topic
     */
    public static final String HYPERIOT_KAFKA_OSGI_BASIC_TOPIC = "hyperiot_layer";
    /**
     * Property used inside OSGi component registration/loading to filter components for specifics topics
     */
    public static final String HYPERIOT_KAFKA_OSGI_CONNECT_URL = "it.acsoftware.hyperiot.kafka.connect.url";

    /**
     * Key Prefix for kafka key, for websocket bridge communication messages
     */
    public static final String KAFKA_WS_BRIDGE_KAFKA_KEY_PREFIX = "WSBIDGE-";

    /**
     * Constant indicating the Kafka Key used to identify message notification for partecipant added on web socket bridge
     */
    public static final String KAFKA_WS_BRIDGE_NEW_KEY_ADDED = KAFKA_WS_BRIDGE_KAFKA_KEY_PREFIX + "NEW_KEY_ADDED";

    /**
     * Constant indicating the Kafka Key used to identify message notification for partecipant added on web socket bridge
     */
    public static final String KAFKA_WS_BRIDGE_KEY_DELETED = KAFKA_WS_BRIDGE_KAFKA_KEY_PREFIX + "KEY_DELETED";

    /**
     * Constant indicating the Kafka Key used to identify message notification for partecipant added on web socket bridge
     */
    public static final String KAFKA_WS_BRIDGE_PARTECIPAND_REQUEST = KAFKA_WS_BRIDGE_KAFKA_KEY_PREFIX + "PARTECIPANT_REQUEST";

    /**
     * Constant indicating the Kafka Key used to identify message notification for partecipant added on web socket bridge
     */
    public static final String KAFKA_WS_BRIDGE_PARTECIPAND_ADDED = KAFKA_WS_BRIDGE_KAFKA_KEY_PREFIX + "PARTECIPANT_ADDED";

    /**
     * Constant indicating the Kafka Key used to identify message notification for owner key found on web socket bridge
     */
    public static final String KAFKA_WS_BRIDGE_OWNER_KEY_FOUND = KAFKA_WS_BRIDGE_KAFKA_KEY_PREFIX + "OWNER_KEY_FOUND";

    /**
     * Constant indicating the Kafka Key used to identify message notification for partecipant disconnected on web socket bridge
     */
    public static final String KAFKA_WS_BRIDGE_PARTECIPAND_DISCONNECTED = KAFKA_WS_BRIDGE_KAFKA_KEY_PREFIX + "PARTECIPANT_DISCONNECTED";

    /**
     * Constant indicating the Kafka Key used to identify message notification for owner disconnected on web socket bridge
     */
    public static final String KAFKA_WS_BRIDGE_OWNER_DISCONNECTED = KAFKA_WS_BRIDGE_KAFKA_KEY_PREFIX + "OWNER_ADDED";


}
