package it.acsoftware.hyperiot.kafka.connector.model;

/**
 * @author Aristide Cittadino Model class for KafkaConnector of HyperIoT
 * platform. This class maps the concept of Message sent from a producer
 * inside HyperIoT platform in order to communicate with other platform
 * components.
 */
public class HyperIoTKafkaMessage {
    /**
     * Kafka Message Key
     */
    private byte[] key;

    /**
     * Kafka message Payload
     */
    private byte[] payload;

    /**
     * Kafka message Topic
     */
    private String topic;

    /**
     * If specified partition where message must be sent
     */
    private int partition = -1;

    /**
     * @param key     Kafka Message Key
     * @param payload Kafka Message Payload
     */
    public HyperIoTKafkaMessage(byte[] key, String topic, byte[] payload) {
        super();
        this.key = key;
        this.payload = payload;
        this.topic = topic;
    }

    /**
     * @param key     Kafka Message Key
     * @param payload Kafka Message Payload
     */
    public HyperIoTKafkaMessage(byte[] key, String topic, int partition, byte[] payload) {
        super();
        this.key = key;
        this.payload = payload;
        this.topic = topic;
        this.partition = partition;
    }

    /**
     * @return Kafka Message Key
     */
    public byte[] getKey() {
        return key;
    }

    /**
     * @return Kafka Message Payload
     */
    public byte[] getPayload() {
        return payload;
    }

    /**
     * @return Kafka Message Topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * @return Partition where messages must be published
     */
    public int getPartition() {
        return partition;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("KAFKA MESSAGE ON TOPIC:").append(topic);
        if (this.getKey() != null)
            sb.append(" KEY IS:").append(new String(key));
        if (this.getPayload() != null)
            sb.append(" MESSAGE IS:").append(new String(payload));
        return sb.toString();
    }

}
