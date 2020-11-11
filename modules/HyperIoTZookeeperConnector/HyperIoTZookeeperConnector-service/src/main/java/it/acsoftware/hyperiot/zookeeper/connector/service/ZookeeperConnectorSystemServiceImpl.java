package it.acsoftware.hyperiot.zookeeper.connector.service;

import it.acsoftware.hyperiot.base.api.HyperIoTLeadershipRegistrar;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseSystemServiceImpl;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.zookeeper.connector.api.ZookeeperConnectorSystemApi;
import it.acsoftware.hyperiot.zookeeper.connector.model.HyperIoTZooKeeperData;
import it.acsoftware.hyperiot.zookeeper.connector.util.HyperIoTZookeeperConstants;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.ServiceTracker;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Aristide Cittadino Implementation class of the ZookeeperConnectorSystemApi
 * interface. This  class is used to implements all additional
 * methods to interact with the persistence layer.
 */
@Component(service = ZookeeperConnectorSystemApi.class, immediate = true)
public final class ZookeeperConnectorSystemServiceImpl extends HyperIoTBaseSystemServiceImpl implements ZookeeperConnectorSystemApi {
    private static Logger log = Logger.getLogger("it.acsoftware.hyperiot");
    private CuratorFramework client;
    private Properties zkProperties;
    private HashMap<String, LeaderLatch> leaderSelectorsMap;
    private ServiceTracker<HyperIoTLeadershipRegistrar, HyperIoTLeadershipRegistrar> leadershipRegistrarServiceTracker;

    public ZookeeperConnectorSystemServiceImpl() {
        log.log(Level.FINE, "Creating service for ZookeeperConnectorSystemApi");
        leaderSelectorsMap = new HashMap<>();
    }

    /**
     * On Activation, zookeeper module loads the configuration, connects to zookeeper and tries to write
     * info about the current container in which it's executed
     */
    @Activate
    public void activate(BundleContext bundleContext) {
        log.log(Level.FINE, "Activating bundle Zookeeper Connector System API");
        this.loadZookeeperConfig();
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 10);
        Object zkConn = zkProperties.getOrDefault(HyperIoTZookeeperConstants.ZOOKEEPER_CONNECTION_URL, "localhost:2181");
        log.log(Level.FINE, "Connecting to zookeeper {0}", new Object[]{zkConn});
        this.client = CuratorFrameworkFactory.newClient((String) (zkProperties.getOrDefault(HyperIoTZookeeperConstants.ZOOKEEPER_CONNECTION_URL, "localhost:2181")), retryPolicy);
        client.start();
        this.registerContainerInfo();

        if (leadershipRegistrarServiceTracker == null) {
            // When called for the first time, create a new service tracker
            // that tracks the availability of a HyperIoTLeadershipRegistrar service.
            leadershipRegistrarServiceTracker = new ServiceTracker<HyperIoTLeadershipRegistrar, HyperIoTLeadershipRegistrar>(
                    bundleContext, HyperIoTLeadershipRegistrar.class, null) {

                // This method is invoked when a service (of the kind tracked) is added
                @Override
                public HyperIoTLeadershipRegistrar addingService(ServiceReference<HyperIoTLeadershipRegistrar> reference) {
                    HyperIoTLeadershipRegistrar result = super.addingService(reference);
                    registerLeadershipComponent(result);
                    return result;
                }

                // This method is invoked when a service is removed
                @Override
                public void removedService(ServiceReference<HyperIoTLeadershipRegistrar> reference,
                                           HyperIoTLeadershipRegistrar service) {
                    unregisterLeadershipComponent(service);
                    super.removedService(reference, service);
                }
            };
        }
        // Now activate (open) the service tracker.
        leadershipRegistrarServiceTracker.open();
    }

    @Deactivate
    public void deactivate() {
        log.log(Level.FINE, "Disconnecting from Zookeeper....");
        this.client.close();
        leadershipRegistrarServiceTracker.close();
    }

    /**
     * Writes to zookeeper current container info
     */
    private void registerContainerInfo() {
        try {
            HyperIoTZooKeeperData data = new HyperIoTZooKeeperData();
            String nodeId = HyperIoTUtil.getNodeId();
            String layer = HyperIoTUtil.getLayer();
            String path = HyperIoTZookeeperConstants.HYPERIOT_ZOOKEEPER_BASE_PATH + "/" + layer + "/" + nodeId;
            data.addinfo("nodeId", nodeId);
            data.addinfo("layer", layer);
            log.log(Level.FINE, "Registering Container info on zookeeper with nodeId: {0} layer: {1} data: \n {2}", new Object[]{nodeId, layer, new String(data.getBytes())});
            this.create(CreateMode.EPHEMERAL, path, data.getBytes(), true);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void registerLeadershipComponent(HyperIoTLeadershipRegistrar component) {
        String zkPath = HyperIoTZookeeperConstants.HYPERIOT_ZOOKEEPER_BASE_PATH + component.getLeadershipPath();
        try {
            this.startLeaderLatch(zkPath);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void unregisterLeadershipComponent(HyperIoTLeadershipRegistrar component) {
        String zkPath = HyperIoTZookeeperConstants.HYPERIOT_ZOOKEEPER_BASE_PATH + component.getLeadershipPath();
        try {
            this.closeLeaderLatch(zkPath);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Loads zookeeper config
     */
    private void loadZookeeperConfig() {
        log.log(Level.FINE, "Zookeeper Properties not cached, reading from .cfg file...");
        BundleContext context = HyperIoTUtil.getBundleContext(this.getClass());
        ServiceReference<?> configurationAdminReference = context
            .getServiceReference(ConfigurationAdmin.class.getName());

        if (configurationAdminReference != null) {
            ConfigurationAdmin confAdmin = (ConfigurationAdmin) context
                .getService(configurationAdminReference);
            try {
                Configuration configuration = confAdmin.getConfiguration(
                    HyperIoTConstants.HYPERIOT_ZOOKEEPER_CONNECTOR_CONFIG_FILE_NAME);
                if (configuration != null && configuration.getProperties() != null) {
                    log.log(Level.FINE, "Reading properties for Zookeeper....");
                    Dictionary<String, Object> dict = configuration.getProperties();
                    List<String> keys = Collections.list(dict.keys());
                    Map<String, Object> dictCopy = keys.stream()
                        .collect(Collectors.toMap(Function.identity(), dict::get));
                    zkProperties = new Properties();
                    zkProperties.putAll(dictCopy);
                    log.log(Level.FINER, "Loaded properties For Zookeeper: {0}", zkProperties);
                }
            } catch (Exception e) {
                log.log(Level.SEVERE,
                    "Impossible to find {0}, please create it!", new Object[]{HyperIoTConstants.HYPERIOT_ZOOKEEPER_CONNECTOR_CONFIG_FILE_NAME, e});
            }
        }
    }

    /**
     * @param mode
     * @param path
     * @param data
     * @param createParentFolders
     * @throws Exception
     */
    public void create(CreateMode mode, String path, byte[] data, boolean createParentFolders) throws Exception {
        if (createParentFolders)
            this.client.create().creatingParentsIfNeeded().withMode(mode).forPath(path, data);
        else
            this.client.create().withMode(mode).forPath(path, data);
    }

    /**
     * @param path
     * @param data
     * @param createParentFolders
     * @throws Exception
     */
    public void create(String path, byte[] data, boolean createParentFolders) throws Exception {
        if (createParentFolders)
            this.client.create().creatingParentsIfNeeded().forPath(path);
        else
            this.client.create().forPath(path);
    }

    /**
     * @param path
     * @return
     * @throws Exception
     */
    public byte[] read(String path) throws Exception {
        return this.client.getData().forPath(path);
    }

    @Override
    public void addListener(LeaderLatchListener listener, String leadershipPath) {
        String zkPath = HyperIoTZookeeperConstants.HYPERIOT_ZOOKEEPER_BASE_PATH + leadershipPath;
        log.info("Adding listener to LeaderLatch on zkPath " + zkPath);
        if (leaderSelectorsMap.containsKey(zkPath))
            leaderSelectorsMap.get(zkPath).addListener(listener);
        else
            log.warning("Could not add listener: LeaderLatch does not exist on zkPath " + zkPath);
    }

    /**
     * @param mutexPath zkNode path
     * @return True if a LeaderLatch exists on path and it has leadership, false otherwise
     */
    public boolean isLeader(String mutexPath) {
        String zkPath = HyperIoTZookeeperConstants.HYPERIOT_ZOOKEEPER_BASE_PATH + mutexPath;
        if (leaderSelectorsMap.containsKey(zkPath))
            return leaderSelectorsMap.get(zkPath).hasLeadership();
        return false;
    }

    /**
     * @param mutexPath
     * @throws Exception
     */
    private void startLeaderLatch(String mutexPath) throws Exception {
        LeaderLatch ll = this.getOrCreateLeaderLatch(mutexPath);
        ll.start();
    }

    /**
     * @param mutexPath
     * @return
     */
    private LeaderLatch getOrCreateLeaderLatch(String mutexPath) {
        LeaderLatch ll = null;
        if (!leaderSelectorsMap.containsKey(mutexPath)) {
            ll = new LeaderLatch(this.client, mutexPath, HyperIoTUtil.getNodeId());
            leaderSelectorsMap.put(mutexPath, ll);
        } else {
            ll = leaderSelectorsMap.get(mutexPath);
        }
        return ll;
    }

    /**
     * @param mutexPath MutexPath
     * @throws Exception Exception
     */
    public void closeLeaderLatch(String mutexPath) throws Exception {
        if (leaderSelectorsMap.containsKey(mutexPath)) {
            LeaderLatch ll = leaderSelectorsMap.remove(mutexPath);
            ll.close();
        }
    }

}
