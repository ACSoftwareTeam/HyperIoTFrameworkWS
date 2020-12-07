package it.acsoftware.hyperiot.kafka.connector.thread;

import it.acsoftware.hyperiot.kafka.connector.api.KafkaMessageReceiver;
import it.acsoftware.hyperiot.kafka.connector.model.HyperIoTKafkaMessage;
import it.acsoftware.hyperiot.kafka.connector.util.HyperIoTKafkaConnectorConstants;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilter;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Aristide Cittadino Consumer Thread which receives message from Kafka
 * and notify OSGi components for the received messages. OSGi components
 * will be notified looking at their OSGi properties.
 */
public class KafkaConsumerThread implements Runnable {
    private static Logger log = Logger.getLogger(KafkaConsumerThread.class.getName());
    private boolean consume;
    private final Consumer<byte[], byte[]> consumer;
    private BundleContext ctx;
    private List<String> topics;

    /**
     * @param props Kafka Connection Properties
     * @param ctx   OSGi bundle context
     */
    public KafkaConsumerThread(Properties props, BundleContext ctx, List<String> topics) {
        super();
        log.log(Level.FINE, "Setting consumer properties {0}", props.toString());
        this.ctx = ctx;
        this.consume = true;
        consumer = new KafkaConsumer<>(props);
        this.topics = topics;
    }

    /**
     * Run method for thread
     */
    @Override
    public void run() {
        if (topics != null && topics.size() > 0) {
            log.log(Level.FINE, "Registering to these topics: {0}", topics.toString());
            consumer.subscribe(topics);
            log.log(Level.FINE, "Start polling Data...");
            while (consume) {
                try {
                    final ConsumerRecords<byte[], byte[]> consumerRecords = consumer
                        .poll(Duration.ofMillis(500));
                    for (TopicPartition partition : consumerRecords.partitions()) {
                        List<ConsumerRecord<byte[], byte[]>> partitionRecords = consumerRecords
                            .records(partition);
                        for (ConsumerRecord<byte[], byte[]> record : partitionRecords) {
                            String topic = record.topic();
                            byte[] key = record.key();
                            byte[] payload = record.value();
                            log.log(Level.FINE, "Got message from Kafka on topic: {0}", topic);
                            HyperIoTKafkaMessage message = new HyperIoTKafkaMessage(key, topic,
                                payload);
                            this.notifyKafkaMessage(message);
                        }
                        long lastOffset = partitionRecords.get(partitionRecords.size() - 1)
                            .offset();
                        consumer.commitSync(Collections.singletonMap(partition,
                            new OffsetAndMetadata(lastOffset + 1)));
                    }
                } catch (WakeupException e) {
                    log.log(Level.INFO, "Waking up Kafka consumer, for other use...");
                } catch (Throwable t) {
                    log.log(Level.SEVERE, t.getMessage(), t);
                }
            }
            this.consumer.close();
            log.log(Level.INFO, "Kafka Consumer stopped.");
        } else {
            log.log(Level.WARNING, "ATTENTION: no topic defined for Kafka consumer");
        }

    }

    /**
     * @param message Message that must be sent
     */
    public void notifyKafkaMessage(HyperIoTKafkaMessage message) {
        try {
            //getting al registered message receiver that wants to receive all keys from a topic or a specific key.
            OSGiFilter specificKeyFilter = OSGiFilterBuilder.createFilter(HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_OSGI_KEY_FILTER, new String(message.getKey()));
            OSGiFilter filter = OSGiFilterBuilder
                .createFilter(HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_OSGI_TOPIC_FILTER,
                    message.getTopic())
                .and(specificKeyFilter);
            String topicFilter = filter.getFilter();
            log.log(Level.FINE, "Searching for components with OSGi filter: {0}", topicFilter);
            Collection<ServiceReference<KafkaMessageReceiver>> references = this.ctx
                .getServiceReferences(KafkaMessageReceiver.class, topicFilter);
            Iterator<ServiceReference<KafkaMessageReceiver>> it = references.iterator();
            while (it.hasNext()) {
                KafkaMessageReceiver messageReceiver = this.ctx.getService(it.next());
                if (messageReceiver != null && message != null) {
                    log.log(Level.FINE, "Kafka receiver found for message on topic: {0}", new Object[]{message.getTopic(), messageReceiver.getClass().getName()});
                    try {
                        messageReceiver.receive(message);
                    } catch (Throwable e) {
                        log.log(Level.SEVERE,
                            "Error while executing notify on component: {0} message is: {1}",
                            new Object[]{messageReceiver.getClass().getName(), e.getMessage(), e});
                    }
                }

            }
        } catch (InvalidSyntaxException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }


    /**
     * Stop message consumption from Kafka
     */
    public void stop() {
        log.log(Level.INFO, "Shutting down kafka Consumer...");
        this.consumer.wakeup();
        this.consume = false;
    }

}
