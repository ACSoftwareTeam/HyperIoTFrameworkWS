package it.acsoftware.hyperiot.sparkmanager;

import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.sparkmanager.api.SparkManagerUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component(service = SparkManagerUtil.class, immediate = true)
public class SparkManagerUtilImpl implements SparkManagerUtil {

    private static Logger log = Logger.getLogger(SparkManagerUtilImpl.class.getName());

    private Properties props;

    @Activate
    private void loadSparkManagerConfiguration() {
        BundleContext context = HyperIoTUtil.getBundleContext(SparkManagerUtilImpl.class);
        log.log(Level.FINE, "Reading SparkManager Properties from .cfg file");
        ServiceReference<?> configurationAdminReference = context.getServiceReference(ConfigurationAdmin.class.getName());
        if (configurationAdminReference != null) {
            ConfigurationAdmin confAdmin = (ConfigurationAdmin) context.getService(configurationAdminReference);
            try {
                Configuration configuration = confAdmin.getConfiguration(SparkManagerConstants.SPARKMANAGER_CONFIG_FILE_NAME);
                if (configuration != null && configuration.getProperties() != null) {
                    log.log(Level.FINE, "Reading properties for SparkManager ....");
                    Dictionary<String, Object> dict = configuration.getProperties();
                    List<String> keys = Collections.list(dict.keys());
                    Map<String, Object> dictCopy = keys.stream().collect(Collectors.toMap(Function.identity(), dict::get));
                    props = new Properties();
                    props.putAll(dictCopy);
                } else
                    log.log(Level.SEVERE, "Impossible to find Configuration admin reference, SparkManager won't start!");
            } catch (IOException e) {
                log.log(Level.SEVERE, "Impossible to find it.acsoftware.hyperiot.sparkmanager.cfg, please create it!", e);
            }
        }
    }

    @Override
    public String getSparkClientVersion() {
        final String DEFAULT_SPARKMANAGER_PROPERTY_SPARK_CLIENT_VERSION = "2.4.5";
        return props.getProperty(SparkManagerConstants.SPARKMANAGER_PROPERTY_SPARK_CLIENT_VERSION,
                DEFAULT_SPARKMANAGER_PROPERTY_SPARK_CLIENT_VERSION);
    }

    @Override
    public boolean getSparkDriverSupervise() {
        final String DEFAULT_SPARKMANAGER_PROPERTY_SPARK_DRIVER_SUPERVISE = "false";
        return Boolean.parseBoolean(props.getProperty(SparkManagerConstants.SPARKMANAGER_PROPERTY_SPARK_DRIVER_SUPERVISE,
                DEFAULT_SPARKMANAGER_PROPERTY_SPARK_DRIVER_SUPERVISE));
    }

    @Override
    public int getSparkEnvLoaded() {
        final String DEFAULT_SPARKMANAGER_PROPERTY_SPARK_ENV_LOADED = "1";
        return Integer.parseInt(props.getProperty(SparkManagerConstants.SPARKMANAGER_PROPERTY_SPARK_ENV_LOADED,
                DEFAULT_SPARKMANAGER_PROPERTY_SPARK_ENV_LOADED));
    }

    @Override
    public String getSparkMasterHostname() {
        final String DEFAULT_SPARKMANAGER_PROPERTY_SPARK_MASTER_HOSTNAME = "http://spark-master";
        return props.getProperty(SparkManagerConstants.SPARKMANAGER_PROPERTY_SPARK_MASTER_HOSTNAME,
                DEFAULT_SPARKMANAGER_PROPERTY_SPARK_MASTER_HOSTNAME);
    }

    @Override
    public int getSparkRestApiPort() {
        final String DEFAULT_SPARKMANAGER_PROPERTY_SPARK_REST_API_PORT = "6066";
        return Integer.parseInt(props.getProperty(SparkManagerConstants.SPARKMANAGER_PROPERTY_SPARK_REST_API_PORT,
                DEFAULT_SPARKMANAGER_PROPERTY_SPARK_REST_API_PORT));
    }

    @Override
    public String getSparkRestApiUrl() {
        return getSparkMasterHostname() + ":" + getSparkRestApiPort();
    }

    @Override
    public String getSparkSubmitDeployMode() {
        final String DEFAULT_SPARKMANAGER_PROPERTY_SPARK_SUBMIT_DEPLOY_MODE = "cluster";
        return props.getProperty(SparkManagerConstants.SPARKMANAGER_PROPERTY_SPARK_SUBMIT_DEPLOY_MODE,
                DEFAULT_SPARKMANAGER_PROPERTY_SPARK_SUBMIT_DEPLOY_MODE);
    }

}
