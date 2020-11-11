package it.acsoftware.hyperiot.kafka.connector.service;

import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseServiceImpl;
import it.acsoftware.hyperiot.kafka.connector.actions.KafkaConnectorAction;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaConnectorApi;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaConnectorSystemApi;
import it.acsoftware.hyperiot.kafka.connector.model.ConnectorConfig;
import it.acsoftware.hyperiot.kafka.connector.model.HyperIoTKafkaPermission;
import it.acsoftware.hyperiot.kafka.connector.model.KafkaConnector;
import org.apache.kafka.clients.admin.CreateAclsResult;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteAclsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.ReceiverRecord;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Aristide Cittadino
 * Implementation class of KafkaConnectorApi
 */
@Component(service = KafkaConnectorApi.class, immediate = true)
public class KafkaConnectorServiceImpl extends HyperIoTBaseServiceImpl implements KafkaConnectorApi {
    private KafkaConnectorSystemApi systemApi;

    public KafkaConnectorServiceImpl() {
        super();
    }

    @Override
    protected KafkaConnectorSystemApi getSystemService() {
        return systemApi;
    }

    @Reference
    public void setSystemApi(KafkaConnectorSystemApi systemApi) {
        this.systemApi = systemApi;
    }

    /**
     * TO DO: manage reactive consumer. Kafka authorization should be managed on Kafka ACLs
     *
     * @param kafkaGroupId           Kafka group identifier
     * @param topics                 List of topic names to consume from
     * @param keyDeserializerClass   Key deserializer class
     * @param valueDeserializerClass Value deserializer class
     * @return Flux
     * @throws ClassNotFoundException In case deserializer not found
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Flux<ReceiverRecord<byte[], byte[]>> consumeReactive(String kafkaGroupId, List<String> topics, Class keyDeserializerClass, Class valueDeserializerClass) throws ClassNotFoundException {
        return systemApi.consumeReactive(kafkaGroupId, topics, keyDeserializerClass, valueDeserializerClass);
    }

    /**
     * TO DO: manage reactive consumer. Kafka authorization should be managed on Kafka ACLs
     *
     * @param kafkaGroupId           kafka GroupId
     * @param topic                  Kafka Topic
     * @param partition              Partition from which data must be consumed
     * @param keyDeserializerClass   Key Deserializer class
     * @param valueDeserializerClass Value Deserializer class
     * @return Flux
     * @throws ClassNotFoundException In case deserializer not found
     */
    @Override
    public Flux<ReceiverRecord<byte[], byte[]>> consumeReactive(String kafkaGroupId, String topic, int partition, Class keyDeserializerClass, Class valueDeserializerClass) throws ClassNotFoundException {
        return systemApi.consumeReactive(kafkaGroupId, topic, partition, keyDeserializerClass, valueDeserializerClass);
    }

    /**
     * @param kafkaGroupId           kafka GroupId
     * @param topicPattern           Kafka Topic Pattern
     * @param keyDeserializerClass   Key Deserializer class
     * @param valueDeserializerClass Value Deserializer class
     * @return
     * @throws ClassNotFoundException
     */
    @Override
    public void consumeReactive(String kafkaGroupId, Pattern topicPattern, Class keyDeserializerClass, Class valueDeserializerClass) throws ClassNotFoundException {
        systemApi.consumeReactive(kafkaGroupId, topicPattern, keyDeserializerClass, valueDeserializerClass);
    }


    /**
     * @param topic         Topic name which must be created
     * @param numPartitions Number of partitions to assign to that topic
     * @param numReplicas   Number of replicas to assing to that topic
     * @return
     */
    @Override
    public CreateTopicsResult adminCreateTopic(HyperIoTContext context, String topic, int numPartitions, short numReplicas) {
        if (HyperIoTSecurityUtil.checkPermission(context, KafkaConnector.class.getName(), HyperIoTActionsUtil.getHyperIoTAction(KafkaConnector.class.getName(), KafkaConnectorAction.ADMIN_KAFKA_TOPICS_ADD))) {
            return this.getSystemService().adminCreateTopic(topic, numPartitions, numReplicas);
        }
        throw new HyperIoTUnauthorizedException();
    }


    /**
     * @param topics        Array of topic that must be created
     * @param numPartitions Array in which in the same position in topic array it must be present the relative numPartitions
     * @param numReplicas   Array in which in the same position in topic array it must be present the relative numReplicas
     * @return
     */
    @Override
    public CreateTopicsResult adminCreateTopic(HyperIoTContext context, String[] topics, int[] numPartitions, short[] numReplicas) {
        if (HyperIoTSecurityUtil.checkPermission(context, KafkaConnector.class.getName(), HyperIoTActionsUtil.getHyperIoTAction(KafkaConnector.class.getName(), KafkaConnectorAction.ADMIN_KAFKA_TOPICS_ADD))) {
            return this.getSystemService().adminCreateTopic(topics, numPartitions, numReplicas);
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * @param topics Topic to be dropped
     * @return
     */
    @Override
    public DeleteTopicsResult adminDropTopic(HyperIoTContext context, List<String> topics) {
        if (HyperIoTSecurityUtil.checkPermission(context, KafkaConnector.class.getName(), HyperIoTActionsUtil.getHyperIoTAction(KafkaConnector.class.getName(), KafkaConnectorAction.ADMIN_KAFKA_TOPICS_DELETE))) {
            return this.getSystemService().adminDropTopic(topics);
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * @param username
     * @param permissions
     * @return
     */
    @Override
    public CreateAclsResult adminAddACLs(HyperIoTContext context, String username, Map<String, HyperIoTKafkaPermission> permissions) {
        if (HyperIoTSecurityUtil.checkPermission(context, KafkaConnector.class.getName(), HyperIoTActionsUtil.getHyperIoTAction(KafkaConnector.class.getName(), KafkaConnectorAction.ADMIN_KAFKA_ACL_ADD))) {
            return this.getSystemService().adminAddACLs(username, permissions);
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * @param username
     * @param permissions
     * @return
     */
    @Override
    public DeleteAclsResult adminDeleteACLs(HyperIoTContext context, String username, Map<String, HyperIoTKafkaPermission> permissions) {
        if (HyperIoTSecurityUtil.checkPermission(context, KafkaConnector.class.getName(), HyperIoTActionsUtil.getHyperIoTAction(KafkaConnector.class.getName(), KafkaConnectorAction.ADMIN_KAFKA_ACL_DELETE))) {
            return this.getSystemService().adminDeleteACLs(username, permissions);
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * @param instanceName
     * @param config
     * @return
     * @throws IOException
     */
    @Override
    public KafkaConnector addNewConnector(HyperIoTContext context, String instanceName, ConnectorConfig config) throws IOException {
        if (HyperIoTSecurityUtil.checkPermission(context, KafkaConnector.class.getName(), HyperIoTActionsUtil.getHyperIoTAction(KafkaConnector.class.getName(), KafkaConnectorAction.ADMIN_KAFKA_CONNECTOR_NEW))) {
            return this.getSystemService().addNewConnector(instanceName, config);
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * @param instanceName
     * @param deleteKafkaTopic
     * @throws IOException
     */
    @Override
    public void deleteConnector(HyperIoTContext context, String instanceName, boolean deleteKafkaTopic) throws IOException {
        if (HyperIoTSecurityUtil.checkPermission(context, KafkaConnector.class.getName(), HyperIoTActionsUtil.getHyperIoTAction(KafkaConnector.class.getName(), KafkaConnectorAction.ADMIN_KAFKA_CONNECTOR_DELETE))) {
            this.getSystemService().deleteConnector(instanceName, deleteKafkaTopic);
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * @param instanceName
     * @return
     * @throws IOException
     */
    @Override
    public KafkaConnector getConnector(HyperIoTContext context, String instanceName) throws IOException {
        if (HyperIoTSecurityUtil.checkPermission(context, KafkaConnector.class.getName(), HyperIoTActionsUtil.getHyperIoTAction(KafkaConnector.class.getName(), KafkaConnectorAction.ADMIN_KAFKA_CONNECTOR_VIEW))) {
            return this.getSystemService().getConnector(instanceName);
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * @return
     * @throws IOException
     */
    @Override
    public List<String> listConnectors(HyperIoTContext context) throws IOException {
        if (HyperIoTSecurityUtil.checkPermission(context, KafkaConnector.class.getName(), HyperIoTActionsUtil.getHyperIoTAction(KafkaConnector.class.getName(), KafkaConnectorAction.ADMIN_KAFKA_CONNECTOR_LIST))) {
            return this.getSystemService().listConnectors();
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * @param instanceName
     * @param config
     * @return
     * @throws IOException
     */
    @Override
    public KafkaConnector updateConnector(HyperIoTContext context, String instanceName, ConnectorConfig config) throws IOException {
        if (HyperIoTSecurityUtil.checkPermission(context, KafkaConnector.class.getName(), HyperIoTActionsUtil.getHyperIoTAction(KafkaConnector.class.getName(), KafkaConnectorAction.ADMIN_KAFKA_CONNECTOR_UPDATE))) {
            return this.getSystemService().updateConnector(instanceName, config);
        }
        throw new HyperIoTUnauthorizedException();
    }
}
