package it.acsoftware.hyperiot.kafka.connector.service.websocket;

import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.service.websocket.HyperIoTWebSocketAbstractSession;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaConnectorApi;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.eclipse.jetty.websocket.api.Session;
import org.osgi.framework.ServiceReference;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.ReceiverRecord;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Author Aristide Cittadino
 * Class which generalize the consumption of messages from kafka and returning them as websocket.
 */
public abstract class KafkaAbstractWebSocketSession extends HyperIoTWebSocketAbstractSession {
    protected static Logger logger = Logger.getLogger("it.acsoftware.hyperiot");

    /**
     * Kafka Connector Api for consuming messages
     */
    protected KafkaConnectorApi kafkaConnectorApi;
    /**
     * Topics for kafka subscription
     */
    protected List<String> topics;

    /**
     * @param session WebSocket session
     */
    public KafkaAbstractWebSocketSession(Session session) {
        super(session, true);
        // Kafka Connector API
        ServiceReference serviceReference = getBundleContext()
            .getServiceReference(KafkaConnectorApi.class);
        kafkaConnectorApi = (KafkaConnectorApi) getBundleContext()
            .getService(serviceReference);
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    /**
     * Utility method reactive flux setup
     */
    Disposable fluxSubscription;


    public void start() {
        if (topics == null)
            throw new HyperIoTRuntimeException("Kafka Topics cannot be null");
        Flux<ReceiverRecord<byte[], byte[]>> messageFlux;
        try {
            String username = this.getContext().getLoggedUsername() + "-" + UUID.randomUUID().toString();
            //each user is different group, so different user can consume from same project differently
            messageFlux = this.getKafkaReactor(username + "-kafka-group");
            messageFlux.doOnError((e) -> {
                onError(e);
            });
            fluxSubscription = messageFlux.subscribe((m) -> {
                try {
                    //sending byte buffer
                    send(m.key(), m.value(), getSession());
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            });
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            getSession().close(1011, "Internal error");
        }
    }

    /**
     * @return Current Session
     */
    @Override
    public Session getSession() {
        return super.getSession();
    }

    /**
     * Dispose session
     */
    @Override
    public void dispose() {
        super.dispose();
        if (fluxSubscription != null) {
            fluxSubscription.dispose();
        }
    }

    /**
     * Can be overidden to Consume in different way
     *
     * @return
     * @throws ClassNotFoundException
     */
    protected Flux<ReceiverRecord<byte[], byte[]>> getKafkaReactor(String consumerGroup) throws ClassNotFoundException {
        return kafkaConnectorApi.consumeReactive(consumerGroup, topics, ByteArrayDeserializer.class, ByteArrayDeserializer.class);
    }

    /**
     * @param e Error Callback
     */
    public abstract void onError(Throwable e);

    /**
     * @param key   Kafka Input key as byte[]
     * @param value Kafka value as byte[]
     * @param s     session to use in order to send via remote
     * @throws IOException
     */
    public abstract void send(byte[] key, byte[] value, Session s) throws IOException;


}
