package it.acsoftware.hyperiot.kafka.connector.service.websocket;

import it.acsoftware.hyperiot.base.service.websocket.bridge.HyperIoTWebSocketBridgeSession;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaConnectorSystemApi;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaMessageReceiver;
import it.acsoftware.hyperiot.kafka.connector.model.HyperIoTKafkaMessage;
import it.acsoftware.hyperiot.kafka.connector.util.HyperIoTKafkaConnectorConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Author Aristide Cittadino
 * This component is used to keep reserved key list updated on all cluster nodes.
 */
@Component(immediate = true)
public class KafkaWebSocketBridgeKeySessionMonitor implements KafkaMessageReceiver {
    private Logger logger = Logger.getLogger("it.acsoftware.hyperiot");
    private KafkaConnectorSystemApi kafkaConnectorSystemApi;
    /**
     *
     */
    private ServiceReference reference;

    private ServiceRegistration registration;

    @Override
    public void receive(HyperIoTKafkaMessage message) {
        String kafkaKey = new String(message.getKey());
        //global message for key reservations between cluster nodes
        if (kafkaKey.equalsIgnoreCase(HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_NEW_KEY_ADDED)) {
            String newKey = new String(message.getPayload());
            HyperIoTWebSocketBridgeSession.onBridgeOwnerKeyAdded(newKey);
        } else if (kafkaKey.equalsIgnoreCase(HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_KEY_DELETED)) {
            String deletedKey = new String(message.getPayload());
            HyperIoTWebSocketBridgeSession.onBridgeOwnerKeyADeleted(deletedKey);
        }
    }

    @Reference
    public void setKafkaConnectorSystemApi(KafkaConnectorSystemApi kafkaConnectorSystemApi) {
        this.kafkaConnectorSystemApi = kafkaConnectorSystemApi;
    }

    @Activate
    public void activate() {
        this.registerToOSGiKafka();
    }

    @Deactivate
    public void deactivate() {
        try {
            registration.unregister();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void registerToOSGiKafka() {
        logger.log(Level.FINE, "Registering web socket to OSGi services...");
        //at the startup time the component will register only to system topics
        Hashtable dictionary = new Hashtable<>();
        List<String> basicTopics = new ArrayList<>();
        List<String> basicKeys = new ArrayList<>();
        basicTopics.add(this.kafkaConnectorSystemApi.getClusterSystemTopic());
        basicKeys.add(HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_NEW_KEY_ADDED);
        basicKeys.add(HyperIoTKafkaConnectorConstants.KAFKA_WS_BRIDGE_KEY_DELETED);
        dictionary.put(HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_OSGI_TOPIC_FILTER, basicTopics.toArray());
        dictionary.put(HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_OSGI_KEY_FILTER, basicKeys.toArray());
        BundleContext ctx = HyperIoTUtil.getBundleContext(KafkaWebSocketBridgeSession.class);
        registration = ctx.registerService(KafkaMessageReceiver.class, this, dictionary);
        reference = registration.getReference();
    }


}
