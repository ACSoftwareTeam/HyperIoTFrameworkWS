package it.acsoftware.hyperiot.kafka.connector.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseSystemServiceImpl;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.base.util.HyperIoTThreadFactoryBuilder;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaConnectorSystemApi;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaMessageReceiver;
import it.acsoftware.hyperiot.kafka.connector.model.ConnectorConfig;
import it.acsoftware.hyperiot.kafka.connector.model.HyperIoTKafkaMessage;
import it.acsoftware.hyperiot.kafka.connector.model.HyperIoTKafkaPermission;
import it.acsoftware.hyperiot.kafka.connector.model.KafkaConnector;
import it.acsoftware.hyperiot.kafka.connector.thread.KafkaConsumerThread;
import it.acsoftware.hyperiot.kafka.connector.util.HyperIoTKafkaConnectorConstants;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilter;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Aristide Cittadino
 * Implementation class of the KafkaConnectorSystemApi interface.
 * This class is used to implements all additional methods
 * to interact with the persistence layer.
 */
@Component(service = KafkaConnectorSystemApi.class, immediate = true, servicefactory = false)
public final class KafkaConnectorSystemServiceImpl extends HyperIoTBaseSystemServiceImpl
    implements KafkaConnectorSystemApi {

    private final String KAFKA_CONNECT_SERVICE_PATH = "/connectors";
    private final String CONNECTOR_ADD_TEMPLATE = "{\"name\":\"%s\", \"config\": {\"max.poll.interval.ms\":\"500\", \"connector.class\":\"\",\"tasks.max\":\"1\"}}";

    private List<KafkaConsumerThread> kcts;
    private Producer<byte[], byte[]> producer;

    private AdminClient adminClient;

    private ExecutorService executor;

    private BundleContext ctx;

    private Properties consumerProperties;
    private Properties producerProperties;
    private Properties adminProperties;
    private Properties connectorProperties;

    private String kafkaConnectUrl;

    // for reactiveStreaming
    private ReceiverOptions<byte[], byte[]> receiverOptions;
    private Scheduler reactorScheduler;

    public KafkaConnectorSystemServiceImpl() {
        super();
        this.consumerProperties = new Properties();
        this.producerProperties = new Properties();
        this.adminProperties = new Properties();
        this.connectorProperties = new Properties();
    }

    /**
     * This method is used if you want register manually an instance as
     * KafkaMessageReceiver and not with @Component annotation.
     *
     * @param receiver Kafka Message Receiver
     * @param topics   Kafka topics on which receiver should receive messages
     * @param props    OSGi properties
     */
    @Override
    public ServiceRegistration<KafkaMessageReceiver> registerKafkaMessageReceiver(
        KafkaMessageReceiver receiver, List<String> topics, Dictionary<String, Object> props) {
        props.put(HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_OSGI_TOPIC_FILTER, topics);
        return ctx.registerService(KafkaMessageReceiver.class, receiver, props);
    }

    /**
     * This method is used if you want register manually an instance as
     * KafkaMessageReceiver and not with @Component annotation.
     *
     * @param registration Kafka Message Receiver
     */
    @Override
    public void unregisterKafkaMessageReceiver(
        ServiceRegistration<KafkaMessageReceiver> registration) {
        registration.unregister();
    }

    /**
     * Method executed on bundle activation. It start a Kafka consumer thread
     * (single thread). When a new message is incoming the thread will notify OSGi
     * components registered to that topic.
     *
     * @param properties Properties
     */
    @Activate
    public void activate(Map<String, Object> properties) {
        getLog().log(Level.INFO, "activating Kafka Connector with properties: {0}", properties);
        this.ctx = HyperIoTUtil.getBundleContext(this.getClass());
        this.loadKafkaConfiguration(HyperIoTUtil.getBundleContext(this));
        this.adminClient = AdminClient.create(adminProperties);
        List<String> basicTopics = this.createBasicTopics();
        this.startConsumingFromKafka(basicTopics);
        int maxThreads = Integer.parseInt(connectorProperties.getProperty(HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_REACTOR_PROP_MAX_CONSUMER_THREADS, "200"));
        this.reactorScheduler = Schedulers.fromExecutor(Executors.newFixedThreadPool(maxThreads));
    }

    /**
     * On Deactivation the consumer thread is stopped
     *
     * @param ctx Bundle Context
     */
    @Deactivate
    public void deactivate(BundleContext ctx) {
        getLog().log(Level.INFO, "deactivating Kafka Connector....");
        this.stopConsumingFromKafka();
        this.reactorScheduler.dispose();
    }

    /**
     * Method for stopping kafka consumer thread
     */
    @Override
    public void stopConsumingFromKafka() {
        getLog().log(Level.FINER, "Stopping admin client from Kafka...");
        try {
            // close call is blocking, so better to put it inside a thread
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    adminClient.close(20, TimeUnit.SECONDS);
                }
            };
            Thread t = new Thread(r);
            t.start();
        } catch (Exception e) {
            getLog().log(Level.WARNING, e.getMessage(), e);
        }
        getLog().log(Level.FINER, "Stopping consuming from Kafka...");
        kcts.stream().forEach(kct -> kct.stop());
        executor.shutdown();
    }

    /**
     * Method for starting consuming from Kafka. Note: the component starts
     * automatically consuming from kafka at bundle activation
     */
    @Override
    public void startConsumingFromKafka(List<String> basicTopics) {
        getLog().log(Level.FINE,
            "Activating bundle Kafka Connector, creating kafka thread with this topics: {0}"
            , basicTopics.toString());
        if (consumerProperties != null && consumerProperties.size() > 0) {
            try {
                consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, Class
                    .forName("org.apache.kafka.common.serialization.ByteArrayDeserializer"));
                consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, Class
                    .forName("org.apache.kafka.common.serialization.ByteArrayDeserializer"));
                //With this group ID Broadcast messaging is working, but only one thread will receive data on each node
                consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG,
                    "hyperiot_" + HyperIoTUtil.getLayer() + "_" + HyperIoTUtil.getNodeId());
                int maxThreads = Integer.parseInt(connectorProperties.getProperty(HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_PROP_MAX_CONSUMER_THREADS, "50"));
                ThreadFactory tf = HyperIoTThreadFactoryBuilder.build("hyperiot-kafka-system-consumer-%d", false);
                executor = Executors.newFixedThreadPool(maxThreads, tf);
                kcts = new ArrayList<>();
                for (int i = 0; i < maxThreads; i++) {
                    Properties threadProperties = new Properties();
                    threadProperties.putAll(consumerProperties);
                    threadProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, HyperIoTUtil.getLayer() + "_" + HyperIoTUtil.getNodeId() + "-" + i);
                    KafkaConsumerThread kct = new KafkaConsumerThread(threadProperties, ctx, basicTopics);
                    kcts.add(kct);
                    executor.submit(kct);
                }
            } catch (Exception e) {
                getLog().log(Level.SEVERE, e.getMessage(), e);
            }
        } else {
            getLog().log(Level.SEVERE, "No Kafka Properties found, Consumer thread did not start!");
        }
    }

    /**
     * Method which produces a message on Kafka and invoke callback
     */
    @Override
    public void produceMessage(HyperIoTKafkaMessage message, Callback callback) {
        if (producerProperties != null && producerProperties.size() > 0) {
            if (this.producer == null) {
                producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                    ByteArraySerializer.class.getName());
                producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                    ByteArraySerializer.class.getName());
                ClassLoader karafClassLoader = Thread.currentThread().getContextClassLoader();
                ClassLoader thisClassLoader = this.getClass().getClassLoader();
                Thread.currentThread().setContextClassLoader(thisClassLoader);
                try {
                    this.producer = new KafkaProducer<>(producerProperties);
                } catch (Exception e) {
                    getLog().log(Level.SEVERE, e.getMessage(), e);
                } finally {
                    Thread.currentThread().setContextClassLoader(karafClassLoader);
                }
            }
            this.produceMessage(message, this.producer, callback);
        }
    }

    public void produceMessage(HyperIoTKafkaMessage message, Producer<byte[], byte[]> producer, Callback callback) {

        ProducerRecord<byte[], byte[]> record = null;
        if (message.getPartition() >= 0) {
            record = new ProducerRecord<>(message.getTopic(), message.getPartition(),
                message.getKey(), message.getPayload());
        } else {
            record = new ProducerRecord<>(message.getTopic(),
                message.getKey(), message.getPayload());
        }

        if (callback != null)
            producer.send(record, callback);
        else
            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception != null)
                        getLog().log(Level.SEVERE, exception.getMessage(), exception);
                    else
                        getLog().log(Level.FINE, metadata.toString());
                }
            });
    }


    /**
     * @return New Kafka Producer
     */
    @Override
    public KafkaProducer<byte[], byte[]> getNewProducer() {
        KafkaProducer<byte[], byte[]> newProducer = null;
        if (producerProperties != null && producerProperties.size() > 0) {
            producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                ByteArraySerializer.class.getName());
            producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                ByteArraySerializer.class.getName());
            ClassLoader karafClassLoader = Thread.currentThread().getContextClassLoader();
            ClassLoader thisClassLoader = this.getClass().getClassLoader();
            Thread.currentThread().setContextClassLoader(thisClassLoader);
            try {
                newProducer = new KafkaProducer<>(producerProperties);
            } catch (Exception e) {
                getLog().log(Level.SEVERE, e.getMessage(), e);
            } finally {
                Thread.currentThread().setContextClassLoader(karafClassLoader);
            }
        }
        return newProducer;
    }

    /**
     * @Return Flux for reactive consuming from Kafka
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Flux<ReceiverRecord<byte[], byte[]>> consumeReactive(String kafkaGroupId, List<String> topics, Class keyDeserializerClass, Class valueDeserializerClass) {
        if (consumerProperties != null && consumerProperties.size() > 0) {
            receiverOptions.consumerProperties().put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializerClass);
            receiverOptions.consumerProperties().put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializerClass);
            receiverOptions.consumerProperties().put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId);
            ReceiverOptions<byte[], byte[]> options = receiverOptions
                .subscription(Collections.synchronizedCollection(topics))
                .addAssignListener(partitions -> getLog().log(Level.FINE, "Consumer Reactive onPartitionsAssigned {0}",
                    partitions))
                .addRevokeListener(partitions -> getLog().log(Level.FINE, "Consumer Reactive onPartitionsRevoked {0}",
                    partitions));
            Flux<ReceiverRecord<byte[], byte[]>> kafkaFlux = KafkaReceiver.create(options).receive().subscribeOn(reactorScheduler);
            return kafkaFlux;
        }
        return null;
    }

    /**
     * @Return Flux for reactive consuming from Kafka
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Flux<ReceiverRecord<byte[], byte[]>> consumeReactive(String kafkaGroupId, String topic, int partition, Class keyDeserializerClass, Class valueDeserializerClass) throws ClassNotFoundException {
        if (consumerProperties != null && consumerProperties.size() > 0) {
            receiverOptions.consumerProperties().put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializerClass);
            receiverOptions.consumerProperties().put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializerClass);
            receiverOptions.consumerProperties().put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId);
            ReceiverOptions<byte[], byte[]> options = receiverOptions.assignment(Collections.singleton(new TopicPartition(topic, partition)));
            Flux<ReceiverRecord<byte[], byte[]>> kafkaFlux = KafkaReceiver.create(options).receive().subscribeOn(reactorScheduler);
            return kafkaFlux;
        }
        return null;
    }

    /**
     * This method should be called once for each topic pattern
     *
     * @Return Flux for reactive consuming from Kafka
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void consumeReactive(String kafkaGroupId, Pattern topicPattern, Class keyDeserializerClass, Class valueDeserializerClass) throws ClassNotFoundException {
        if (consumerProperties != null && consumerProperties.size() > 0) {
            receiverOptions.consumerProperties().put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializerClass);
            receiverOptions.consumerProperties().put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializerClass);
            receiverOptions.consumerProperties().put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId);
            ReceiverOptions<byte[], byte[]> options = receiverOptions.subscription(topicPattern);

            KafkaReceiver.create(receiverOptions)
                .receive().subscribeOn(reactorScheduler).parallel().runOn(Schedulers.elastic()).subscribe(m -> {
                String topic = m.topic();
                byte[] key = m.key();
                int partition = m.partition();
                HyperIoTKafkaMessage message = new HyperIoTKafkaMessage(key, topic, partition, m.value());
                getLog().log(Level.FINE, "Got message from kafka: {0} on partition {1} with key {2}", new Object[]{message.toString(), partition, new String(m.key())});

                OSGiFilter specificKeyFilter = OSGiFilterBuilder.createFilter(HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_OSGI_KEY_FILTER, new String(m.key()));
                OSGiFilter filter = OSGiFilterBuilder
                    .createFilter(HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_OSGI_TOPIC_FILTER,
                        m.topic())
                    .and(specificKeyFilter);
                String topicFilter = filter.getFilter();
                getLog().log(Level.FINE, "Searching for components with OSGi filter: {0}", topicFilter);
                try {
                    Collection<ServiceReference<KafkaMessageReceiver>> references = this.ctx.getServiceReferences(KafkaMessageReceiver.class, topicFilter);
                    references.stream().parallel().forEach(reference -> {
                        getLog().log(Level.FINE, "Receiver Found , invoking receive for key {0}", new Object[]{new String(m.key())});
                        KafkaMessageReceiver messageReceiver = (KafkaMessageReceiver) this.ctx.getService(reference);
                        if (messageReceiver != null)
                            messageReceiver.receive(message);
                    });
                } catch (InvalidSyntaxException e) {
                    getLog().log(Level.SEVERE, e.getMessage(), e);
                }
            });
        }
    }


    /**
     * Method which produces a message on Kafka without callback
     */
    @Override
    public void produceMessage(HyperIoTKafkaMessage message) {
        this.produceMessage(message, null);
    }

    /**
     * @param context Bundle Context
     */
    private void loadKafkaConfiguration(BundleContext context) {
        getLog().log(Level.FINE, "Kafka Properties not cached, reading from .cfg file...");
        ServiceReference<?> configurationAdminReference = context
            .getServiceReference(ConfigurationAdmin.class.getName());

        if (configurationAdminReference != null) {
            ConfigurationAdmin confAdmin = (ConfigurationAdmin) context
                .getService(configurationAdminReference);
            try {
                Configuration configuration = confAdmin.getConfiguration(
                    HyperIoTConstants.HYPERIOT_KAFKA_CONNECTOR_CONFIG_FILE_NAME);
                if (configuration != null && configuration.getProperties() != null) {
                    getLog().log(Level.FINE, "Reading properties for Kafka....");
                    Dictionary<String, Object> dict = configuration.getProperties();
                    List<String> keys = Collections.list(dict.keys());
                    Map<String, Object> dictCopy = keys.stream()
                        .collect(Collectors.toMap(Function.identity(), dict::get));
                    this.kafkaConnectUrl = (String) dictCopy.getOrDefault(HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_OSGI_CONNECT_URL, "localhost:8080");
                    Iterator<String> it = dictCopy.keySet().iterator();
                    while (it.hasNext()) {
                        String propName = it.next();
                        if (propName.startsWith(
                            HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_PROPS_CONSUMER_PREFIX)) {
                            getLog().log(Level.FINE, "Reading consumer property for Kafka: {0}", propName);
                            consumerProperties.put(propName.replaceAll(
                                HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_PROPS_CONSUMER_PREFIX,
                                "").substring(1), dictCopy.get(propName));
                        } else if (propName.startsWith(
                            HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_PROPS_PRODUCER_PREFIX)) {
                            getLog().log(Level.FINE, "Reading producer property for Kafka: {0}", propName);
                            producerProperties.put(propName.replaceAll(
                                HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_PROPS_PRODUCER_PREFIX,
                                "").substring(1), dictCopy.get(propName));
                        } else if (propName.startsWith(
                            HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_PROPS_ADMIN_PREFIX)) {
                            getLog().log(Level.FINE, "Reading admin property for Kafka: {0}", propName);
                            adminProperties.put(propName.replaceAll(
                                HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_PROPS_ADMIN_PREFIX,
                                "").substring(1), dictCopy.get(propName));
                        } else if (propName.startsWith(
                            HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_PROPS_GLOBAL_PREFIX)) {
                            getLog().log(Level.FINE, "Reading global property for Kafka: {0}", propName);
                            String globalPropName = propName.replaceAll(
                                HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_PROPS_GLOBAL_PREFIX,
                                "").substring(1);
                            consumerProperties.put(globalPropName, dictCopy.get(propName));
                            producerProperties.put(globalPropName, dictCopy.get(propName));
                            adminProperties.put(globalPropName, dictCopy.get(propName));
                        } else {
                            connectorProperties.put(propName, dictCopy.get(propName));
                        }
                    }
                    this.receiverOptions = ReceiverOptions.create(consumerProperties);
                    return;
                } else {
                    getLog().log(Level.SEVERE,
                        "Impossible to find Configuration admin reference, kafka consumer won't start!");
                }
            } catch (IOException e) {
                getLog().log(Level.SEVERE,
                    "Impossible to find {0}", new Object[]{HyperIoTConstants.HYPERIOT_KAFKA_CONNECTOR_CONFIG_FILE_NAME, e});

            }
        }
        getLog().log(Level.SEVERE,
            "Impossible to find {0}", new Object[]{HyperIoTConstants.HYPERIOT_KAFKA_CONNECTOR_CONFIG_FILE_NAME});

    }

    /**
     * @return cluster system topic
     */
    @Override
    public String getClusterSystemTopic() {
        return HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_OSGI_BASIC_TOPIC + "_"
            + HyperIoTUtil.getLayer();
    }

    /**
     * This method creates automatically basic topic needed by current instance in
     * order to communicate with HyperIoT Infrastructure
     */
    private List<String> createBasicTopics() {
        getLog().log(Level.INFO, "Creating basic topic on kafka if they do not exists...");
        // register on hyperiot_layer_<layer>
        String[] topics = new String[2];
        int[] numPartitions = new int[2];
        short[] numReplicas = new short[2];
        topics[0] = getClusterSystemTopic();
        topics[1] = getClusterSystemTopic() + "_" + HyperIoTUtil.getNodeId();
        numPartitions[0] = 1;
        numPartitions[1] = 1;
        numReplicas[0] = (short) 1;
        numReplicas[1] = (short) 1;
        this.adminCreateTopic(topics, numPartitions, numReplicas);
        List<String> topicList = Arrays.asList(topics);
        getLog().log(Level.FINE, "Topics for this node are: {0}", topicList.toString());
        return topicList;
    }

    /**
     * @param topic         Topic name which must be created
     * @param numPartitions Number of partitions to assign to that topic
     * @param numReplicas   Number of replicas to assing to that topic
     * @return Future cointaining the execution result
     */
    @Override
    public CreateTopicsResult adminCreateTopic(String topic, int numPartitions, short numReplicas) {
        NewTopic newTopic = new NewTopic(topic, numPartitions, numReplicas);
        List<NewTopic> topics = new ArrayList<>();
        topics.add(newTopic);
        return this.adminClient.createTopics(topics);
    }

    /**
     * @param topics        Array of topic that must be created
     * @param numPartitions Array in which in the same position in topic array it
     *                      must be present the relative numPartitions
     * @param numReplicas   Array in which in the same position in topic array it
     *                      must be present the relative numReplicas
     * @return Future cointaining the execution result
     */
    @Override
    public CreateTopicsResult adminCreateTopic(String[] topics, int[] numPartitions,
                                               short[] numReplicas) {
        if (topics.length != numPartitions.length || topics.length != numReplicas.length)
            return null;

        List<NewTopic> topicsList = new ArrayList<>();
        for (int i = 0; i < topics.length; i++) {
            getLog().log(Level.FINE, "Topic to be created: {0}", topics[i]);
            topicsList.add(new NewTopic(topics[i], numPartitions[i], numReplicas[i]));
        }
        getLog().log(Level.FINE, "Invoking Kafka ADMIN Client..");

        CreateTopicsResult result = this.adminClient.createTopics(topicsList);
        return result;
    }

    /**
     * @param topics List of topics to be dropped
     * @return Future containing the execution result
     */
    @Override
    public DeleteTopicsResult adminDropTopic(List<String> topics) {
        return this.adminClient.deleteTopics(topics);
    }

    /**
     * Method for controlling access on Kafka resources
     *
     * @param username
     * @param permissions
     * @return
     */
    public CreateAclsResult adminAddACLs(String username, Map<String, HyperIoTKafkaPermission> permissions) {
        Iterator<String> it = permissions.keySet().iterator();
        List<AclBinding> acls = new ArrayList<>();
        while (it.hasNext()) {
            HyperIoTKafkaPermission permission = permissions.get(it.next());
            ResourcePattern resourcePattern = new ResourcePattern(ResourceType.TOPIC, permission.getTopic(), permission.getPatternType());
            AclBinding userAcl = new AclBinding(resourcePattern, new AccessControlEntry(username, "*", permission.getAclOperation(), permission.getAclPermissionType()));
            acls.add(userAcl);
        }
        CreateAclsOptions options = new CreateAclsOptions();
        //no options
        return this.adminClient.createAcls(acls, options);
    }

    /**
     * @return DeleteAclsResult
     */
    public DeleteAclsResult adminDeleteACLs(String username, Map<String, HyperIoTKafkaPermission> permissions) {
        Iterator<String> it = permissions.keySet().iterator();
        List<AclBindingFilter> acls = new ArrayList<>();
        while (it.hasNext()) {
            HyperIoTKafkaPermission permission = permissions.get(it.next());
            ResourcePatternFilter resourcePattern = new ResourcePatternFilter(ResourceType.TOPIC, permission.getTopic(), permission.getPatternType());
            AclBindingFilter userAcl = new AclBindingFilter(resourcePattern, new AccessControlEntryFilter(username, "*", permission.getAclOperation(), permission.getAclPermissionType()));
            acls.add(userAcl);
        }
        DeleteAclsOptions options = new DeleteAclsOptions();
        //no options
        return this.adminClient.deleteAcls(acls, options);
    }

    /**
     * @param instanceName
     * @param config
     * @return
     * @throws IOException
     */
    @Override
    public KafkaConnector addNewConnector(String instanceName, ConnectorConfig config) throws IOException {
        // TO DO: define post data
        String postData = String.format(CONNECTOR_ADD_TEMPLATE, config.getName(), config.getMaxPollIntervalMs(), config.getConnectorClass());
        String response = kafkaConnectPost(kafkaConnectUrl, postData);
        if (response.indexOf("error_code") > 0) {
            throw new IOException(response);
        } else {
            ObjectMapper mapper = new ObjectMapper();
            KafkaConnector connector = mapper.readValue(response, KafkaConnector.class);
            return connector;
        }
    }

    /**
     * @param instanceName
     * @param deleteKafkaTopic
     * @throws IOException
     */
    @Override
    public void deleteConnector(String instanceName, boolean deleteKafkaTopic) throws IOException {
        String response = kafkaConnectDelete(kafkaConnectUrl, instanceName);
        if (response.indexOf("error_code") > 0) {
            throw new IOException(response);
        } else if (deleteKafkaTopic) {
            // TODO: delete Kafka topic as well
        }
    }

    /**
     * @param instanceName
     * @return
     * @throws IOException
     */
    @Override
    public KafkaConnector getConnector(String instanceName) throws IOException {
        String response = kafkaConnectGet(kafkaConnectUrl, instanceName, null);
        if (response.indexOf("error_code") > 0) {
            throw new IOException(response);
        } else {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response, KafkaConnector.class);
        }
    }

    /**
     * @return
     * @throws IOException
     */
    @Override
    public List<String> listConnectors() throws IOException {
        String response = kafkaConnectGet(kafkaConnectUrl, "", null);
        if (response.indexOf("error_code") > 0) {
            throw new IOException(response);
        } else {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response, new TypeReference<List<String>>() {
            });
        }
    }

    /**
     * @param instanceName
     * @param config
     * @return
     * @throws IOException
     */
    @Override
    public KafkaConnector updateConnector(String instanceName, ConnectorConfig config) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String putData = mapper.writeValueAsString(config);
        String response = kafkaConnectPut(kafkaConnectUrl, instanceName + "/config", putData);
        if (response.indexOf("error_code") > 0) {
            throw new IOException(response);
        } else {
            return mapper.readValue(response, KafkaConnector.class);
        }
    }

    // Utility methods

    /**
     * @param kafkaConnectUrl
     * @param path
     * @param putData
     * @return
     * @throws IOException
     */
    private String kafkaConnectPut(String kafkaConnectUrl, String path, String putData) throws IOException {
        HttpPut httpPut = new HttpPut(kafkaConnectUrl + KAFKA_CONNECT_SERVICE_PATH + "/" + path);
        httpPut.setHeader("Accept", "application/json");
        httpPut.setHeader("Content-type", "application/json");
        StringEntity entity = new StringEntity(putData);
        httpPut.setEntity(entity);
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(httpPut);
        String responseText = getKafkaConnectResponseText(response);
        response.close();
        client.close();
        return responseText;
    }

    /**
     * @param kafkaConnectUrl
     * @param postData
     * @return
     * @throws IOException
     */
    private String kafkaConnectPost(String kafkaConnectUrl, String postData) throws IOException {
        HttpPost httpPost = new HttpPost(kafkaConnectUrl + KAFKA_CONNECT_SERVICE_PATH);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        // set post data
        StringEntity entity = new StringEntity(postData);
        httpPost.setEntity(entity);
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(httpPost);
        String responseText = getKafkaConnectResponseText(response);
        response.close();
        client.close();
        return responseText;
    }

    /**
     * @param kafkaConnectUrl
     * @param path
     * @param parameters
     * @return
     * @throws IOException
     */
    private String kafkaConnectGet(String kafkaConnectUrl, String path, Map<String, String> parameters) throws
        IOException {
        HttpGet httpGet = new HttpGet(kafkaConnectUrl + KAFKA_CONNECT_SERVICE_PATH + "/" + path);
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(httpGet);
        String responseText = getKafkaConnectResponseText(response);
        response.close();
        client.close();
        return responseText;
    }

    /**
     * @param kafkaConnectUrl
     * @param arguments
     * @return
     * @throws IOException
     */
    private String kafkaConnectDelete(String kafkaConnectUrl, String arguments) throws IOException {
        HttpDelete httpDelete = new HttpDelete(kafkaConnectUrl + KAFKA_CONNECT_SERVICE_PATH + "/" + arguments);
        httpDelete.setHeader("Accept", "application/json");
        // set post data
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(httpDelete);
        String responseText = getKafkaConnectResponseText(response);
        response.close();
        client.close();
        return responseText;
    }

    /**
     * @param response
     * @return
     * @throws IOException
     */
    private String getKafkaConnectResponseText(CloseableHttpResponse response) throws IOException {
        if (response.getEntity() == null) return "";
        BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        return content.toString();
    }


}
