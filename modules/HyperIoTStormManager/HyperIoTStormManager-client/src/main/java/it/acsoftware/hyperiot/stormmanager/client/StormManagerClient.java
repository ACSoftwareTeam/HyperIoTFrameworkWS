package it.acsoftware.hyperiot.stormmanager.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.stormmanager.api.StormClient;
import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.flux.FluxBuilder;
import org.apache.storm.flux.model.ExecutionContext;
import org.apache.storm.flux.model.TopologyDef;
import org.apache.storm.flux.parser.FluxParser;
import org.apache.storm.generated.*;
import org.apache.storm.thrift.TException;
import org.apache.storm.utils.NimbusClient;
import org.apache.storm.utils.Utils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component(immediate = true, service = StormClient.class)
public class StormManagerClient implements StormClient {
    public static final String HYPERIOT_PROPERTY_STORM_NIMBUS_SEEDS = "it.acsoftware.hyperiot.stormmanager.nimbus.seeds";
    public static final String HYPERIOT_STORM_TOPOLOGY_PROP_HASHCODE = "it.acsoftware.hyperiot.storm.topology.hashcode";
    public static ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = Logger.getLogger("it.acsoftware.hyperiot");
    private Map stormConfig;
    private final boolean dumpYaml = true;
    private final boolean envFilter = true;
    private String topologyJarFilePath;
    private String topologyJarFileName;

    public StormManagerClient() {
    }

    @Activate
    public synchronized void activate() {
        List<String> nimbusSeeds = Arrays.asList(((String) HyperIoTUtil.getHyperIoTProperty(HYPERIOT_PROPERTY_STORM_NIMBUS_SEEDS)).split(","));
        this.topologyJarFilePath = (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.stormmanager.topology.dir");
        this.topologyJarFileName = (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.stormmanager.topology.jar");
        ClassLoader karafClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader thisClassLoader = this.getClass().getClassLoader();
        Thread.currentThread().setContextClassLoader(thisClassLoader);
        try {
            stormConfig = Utils.readStormConfig();
            stormConfig.put(Config.NIMBUS_SEEDS, nimbusSeeds);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(karafClassLoader);
        }
    }


    @Deactivate
    public synchronized void deactivate() {
        //Removing topology jar file
        try {
            Files.delete(Paths.get(this.topologyJarFilePath));
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Gets the list of alive topologies.
     *
     * @return List of TopologySummary
     * @throws TException
     */
    public List<TopologySummary> getTopologyList() throws TException {
        Nimbus.Client client = NimbusClient.getConfiguredClient(stormConfig).getClient();
        ClusterSummary clusterSummary = client.getClusterInfo();
        return clusterSummary.get_topologies();
    }

    /**
     * Gets the topology instance object.
     *
     * @param topologyId Topology ID
     * @return StormTopology object
     * @throws TException
     */
    public StormTopology getTopology(String topologyId) throws TException {
        Nimbus.Client client = NimbusClient.getConfiguredClient(stormConfig).getClient();
        return client.getTopology(topologyId);
    }

    /**
     * Gets the topology configuration.
     *
     * @param topologyId Topology ID
     * @return The JSON serialized topology configuration
     * @throws TException
     */
    public String getTopologyConfig(String topologyId) throws TException {
        Nimbus.Client client = NimbusClient.getConfiguredClient(stormConfig).getClient();
        return client.getTopologyConf(topologyId);
    }

    /**
     * Gets the topology info.
     *
     * @param topologyId Topology ID
     * @return TopologyInfo object
     * @throws TException
     */
    public TopologyInfo getTopologyInfo(String topologyId) throws TException {
        Nimbus.Client client = NimbusClient.getConfiguredClient(stormConfig).getClient();
        return client.getTopologyInfo(topologyId);
    }

    /**
     * Activates the topology with the given name.
     *
     * @param topologyName Topology name
     * @throws TException
     */
    public void activate(String topologyName) throws TException {
        Nimbus.Client client = NimbusClient.getConfiguredClient(stormConfig).getClient();
        client.activate(topologyName);
    }

    /**
     * Deactivates the topology with the given name.
     *
     * @param topologyName Topology name
     * @throws TException
     */
    public void deactivate(String topologyName) throws TException {
        Nimbus.Client client = NimbusClient.getConfiguredClient(stormConfig).getClient();
        client.deactivate(topologyName);
    }

    /**
     * Kills the topology with the given name.
     *
     * @param topologyName Topology name
     * @throws TException
     */
    public void killTopology(String topologyName) throws TException {
        Nimbus.Client client = NimbusClient.getConfiguredClient(stormConfig).getClient();
        KillOptions killOpts = new KillOptions();
        killOpts.set_wait_secs(0); // time to wait before killing
        client.killTopologyWithOpts(topologyName, killOpts); //provide topology name
    }

    public int getTopologyConfigHashCode(TopologySummary summary) {
        if (summary != null) {
            try {
                String conf = this.getTopologyConfig(summary.get_id());
                Map<String, Object> topologyConfig = mapper.readValue(conf, Map.class);
                return (Integer) topologyConfig.get(HYPERIOT_STORM_TOPOLOGY_PROP_HASHCODE);
            } catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return -1;
    }

    /**
     * Submits a topology with initial INACTIVE status.
     *
     * @param topologyProperties Topology properties text
     * @param topologyYaml       Topology YAML definition text
     * @return The submitted topology name
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws NoSuchFieldException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     * @throws InvalidTopologyException
     * @throws AuthorizationException
     * @throws AlreadyAliveException
     */
    public synchronized String submitTopology(String topologyProperties, String topologyYaml, int topologyConfigHashCode)
            throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException, NoSuchMethodException, ClassNotFoundException, InvalidTopologyException, AuthorizationException, AlreadyAliveException {

        // store topology property and yaml files in a temporary folder

        String randomUUIDString = UUID.randomUUID().toString();

        Path topologyPropsPath = writeTempFile(randomUUIDString, ".properties", topologyProperties.getBytes());
        Properties filterProps = FluxParser.parseProperties(
                topologyPropsPath.toAbsolutePath().toString(),
                false
        );
        topologyPropsPath.toFile().delete();

        Path topologyFluxPath = writeTempFile(randomUUIDString, ".yaml", topologyYaml.getBytes());
        TopologyDef topologyDef = FluxParser.parseFile(
                topologyFluxPath.toAbsolutePath().toString(),
                dumpYaml,
                true,
                filterProps, envFilter
        );
        topologyFluxPath.toFile().delete();

        String topologyName = topologyDef.getName();
        // merge contents of `config` into topology config
        Config conf = new Config();
        conf.putAll(stormConfig);
        conf.putAll(FluxBuilder.buildConfig(topologyDef));
        conf.put(HYPERIOT_STORM_TOPOLOGY_PROP_HASHCODE, topologyConfigHashCode);
        // Kills topology `topologyName` before submitting if already exists
        try {
            killTopology(topologyName);
            Thread.sleep(1000);
        } catch (Exception e) {
            //e.printStackTrace();
        }

        // Create context and topology instance
        ExecutionContext context = new ExecutionContext(topologyDef, conf);
        StormTopology topology = FluxBuilder.buildTopology(context);
        SubmitOptions submitOptions = new SubmitOptions(TopologyInitialStatus.ACTIVE);
        // submit the topology
        ClassLoader karafClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader thisClassLoader = this.getClass().getClassLoader();
        System.setProperty("storm.jar", this.topologyJarFilePath + "/" + this.topologyJarFileName);
        Thread.currentThread().setContextClassLoader(thisClassLoader);
        try {
            StormSubmitter.submitTopology(topologyName, conf, topology, submitOptions, null);
        } finally {
            Thread.currentThread().setContextClassLoader(karafClassLoader);
        }
        return topologyName;
    }


    private Path writeTempFile(String fileName, String fileExtension, byte[] fileContent) throws IOException {
        final Path path = Files.createTempFile(fileName, fileExtension);
        //Writing data here
        byte[] buf = fileContent;
        Files.write(path, buf);
        //Delete file on application exit
        //path.toFile().deleteOnExit();
        return path;
    }


}
