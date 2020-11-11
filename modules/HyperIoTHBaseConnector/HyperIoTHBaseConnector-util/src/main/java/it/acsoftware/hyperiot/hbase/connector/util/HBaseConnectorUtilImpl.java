package it.acsoftware.hyperiot.hbase.connector.util;

import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.io.IOException;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component(service = HBaseConnectorUtil.class, immediate = true)
public class HBaseConnectorUtilImpl implements HBaseConnectorUtil {

    private static Logger log = Logger.getLogger("it.acsoftware.hyperiot");

    private Properties props;

    @Activate
    private void loadHBaseConnectorConfiguration() {
        BundleContext context = HyperIoTUtil.getBundleContext(HBaseConnectorUtilImpl.class);
        log.log(Level.FINE, "Reading HBase Connector Properties from .cfg file");
        ServiceReference<?> configurationAdminReference = context.getServiceReference(ConfigurationAdmin.class.getName());
        if (configurationAdminReference != null) {
            ConfigurationAdmin confAdmin = (ConfigurationAdmin) context.getService(configurationAdminReference);
            try {
                Configuration configuration = confAdmin.getConfiguration(HBaseConnectorConstants.HBASE_CONNECTOR_CONFIG_FILE_NAME);
                if (configuration != null && configuration.getProperties() != null) {
                    log.log(Level.FINE, "Reading properties for HBase Connector ....");
                    Dictionary<String, Object> dict = configuration.getProperties();
                    List<String> keys = Collections.list(dict.keys());
                    Map<String, Object> dictCopy = keys.stream().collect(Collectors.toMap(Function.identity(), dict::get));
                    props = new Properties();
                    props.putAll(dictCopy);
                } else
                    log.log(Level.SEVERE, "Impossible to find Configuration admin reference, hbase connector won't start!");
            } catch (IOException e) {
                log.log(Level.SEVERE, "Impossible to find it.acsoftware.hyperiot.hbase.connector.cfg, please create it!", e);
            }
        }
    }

    public long getAwaitTermination() {
        final String DEFAULT_HBASE_CONNECTOR_PROPERTY_AWAIT_TERMINATION = "1000";
        return Long.parseLong(props.getProperty(HBaseConnectorConstants.HBASE_CONNECTOR_PROPERTY_AWAIT_TERMINATION,
                DEFAULT_HBASE_CONNECTOR_PROPERTY_AWAIT_TERMINATION));
    }

    public boolean getClusterDistributed() {
        final String DEFAULT_HBASE_CONNECTOR_PROPERTY_CLUSTER_DISTRIBUTED = "false";
        return Boolean.parseBoolean(props.getProperty(HBaseConnectorConstants.HBASE_CONNECTOR_PROPERTY_CLUSTER_DISTRIBUTED,
                DEFAULT_HBASE_CONNECTOR_PROPERTY_CLUSTER_DISTRIBUTED));
    }

    public int getCorePoolSize() {
        final String DEFAULT_HBASE_CONNECTOR_PROPERTY_CORE_POOL_SIZE = "10";
        return Integer.parseInt(props.getProperty(HBaseConnectorConstants.HBASE_CONNECTOR_PROPERTY_CORE_POOL_SIZE,
                DEFAULT_HBASE_CONNECTOR_PROPERTY_CORE_POOL_SIZE));
    }

    public long getKeepAliveTime() {
        final String DEFAULT_HBASE_CONNECTOR_PROPERTY_KEEP_ALIVE_TIME = "0";
        return Long.parseLong(props.getProperty(HBaseConnectorConstants.HBASE_CONNECTOR_PROPERTY_KEEP_ALIVE_TIME,
                DEFAULT_HBASE_CONNECTOR_PROPERTY_KEEP_ALIVE_TIME));
    }

    public String getMaster() {
        final String DEFAULT_HBASE_CONNECTOR_PROPERTY_MASTER = "hbase-test.hyperiot.cloud:1600";
        return props.getProperty(HBaseConnectorConstants.HBASE_CONNECTOR_PROPERTY_MASTER,
                DEFAULT_HBASE_CONNECTOR_PROPERTY_MASTER);
    }

    public String getMasterHostname() {
        final String DEFAULT_HBASE_CONNECTOR_PROPERTY_MASTER_HOSTNAME = "hbase-test.hyperiot.cloud";
        return props.getProperty(HBaseConnectorConstants.HBASE_CONNECTOR_PROPERTY_MASTER_HOSTNAME,
                DEFAULT_HBASE_CONNECTOR_PROPERTY_MASTER_HOSTNAME);
    }

    public int getMasterInfoPort() {
        final String DEFAULT_HBASE_CONNECTOR_PROPERTY_MASTER_INFO_PORT = "16010";
        return Integer.parseInt(props.getProperty(HBaseConnectorConstants.HBASE_CONNECTOR_PROPERTY_MASTER_INFO_PORT,
                DEFAULT_HBASE_CONNECTOR_PROPERTY_MASTER_INFO_PORT));
    }

    public int getMasterPort() {
        final String DEFAULT_HBASE_CONNECTOR_PROPERTY_MASTER_PORT = "16000";
        return Integer.parseInt(props.getProperty(HBaseConnectorConstants.HBASE_CONNECTOR_PROPERTY_MASTER_PORT,
                DEFAULT_HBASE_CONNECTOR_PROPERTY_MASTER_PORT));
    }

    public int getMaximumPoolSize() {
        final String DEFAULT_HBASE_CONNECTOR_PROPERTY_MAXIMUM_POOL_SIZE = "10";
        return Integer.parseInt(props.getProperty(HBaseConnectorConstants.HBASE_CONNECTOR_PROPERTY_MAXIMUM_POOL_SIZE,
                DEFAULT_HBASE_CONNECTOR_PROPERTY_MAXIMUM_POOL_SIZE));
    }

    public int getMaxScanPageSize() {
        final String DEFAULT_HBASE_CONNECTOR_PROPERTY_MAX_SCAN_PAGE_SIZE = "1000";
        return Integer.parseInt(props.getProperty(HBaseConnectorConstants.HBASE_CONNECTOR_PROPERTY_MAX_SCAN_PAGE_SIZE,
                DEFAULT_HBASE_CONNECTOR_PROPERTY_MAX_SCAN_PAGE_SIZE));
    }

    public int getRegionserverInfoPort() {
        final String DEFAULT_HBASE_CONNECTOR_PROPERTY_REGIONSERVER_INFO_PORT = "16030";
        return Integer.parseInt(props.getProperty(HBaseConnectorConstants.HBASE_CONNECTOR_PROPERTY_REGIONSERVER_INFO_PORT,
                DEFAULT_HBASE_CONNECTOR_PROPERTY_REGIONSERVER_INFO_PORT));
    }

    public int getRegionserverPort() {
        final String DEFAULT_HBASE_CONNECTOR_PROPERTY_REGIONSERVER_PORT = "16020";
        return Integer.parseInt(props.getProperty(HBaseConnectorConstants.HBASE_CONNECTOR_PROPERTY_REGIONSERVER_PORT,
                DEFAULT_HBASE_CONNECTOR_PROPERTY_REGIONSERVER_PORT));
    }

    public String getRootdir() {
        final String DEFAULT_HBASE_CONNECTOR_PROPERTY_ROOTDIR = "hdfs://namenode:8020/hbase";
        return props.getProperty(HBaseConnectorConstants.HBASE_CONNECTOR_PROPERTY_ROOTDIR,
                DEFAULT_HBASE_CONNECTOR_PROPERTY_ROOTDIR);
    }

    public String getZookeeperQuorum() {
        final String DEFAULT_HBASE_CONNECTOR_PROPERTY_ZOOKEEPER_QUORUM = "zookeeper-1.hyperiot.com";
        return props.getProperty(HBaseConnectorConstants.HBASE_CONNECTOR_PROPERTY_ZOOKEEPER_QUORUM,
                DEFAULT_HBASE_CONNECTOR_PROPERTY_ZOOKEEPER_QUORUM);
    }

}
