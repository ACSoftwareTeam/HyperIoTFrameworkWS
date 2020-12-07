package it.acsoftware.hyperiot.hadoopmanager.util;

import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerUtil;
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

@Component(service = HadoopManagerUtil.class, immediate = true)
public class HadoopManagerUtilImpl implements HadoopManagerUtil {

    private static final Logger LOGGER = Logger.getLogger(HadoopManagerUtilImpl.class.getName());

    private Properties props;

    @Activate
    private void loadHadoopManagerConfiguration() {
        BundleContext context = HyperIoTUtil.getBundleContext(HadoopManagerUtilImpl.class);
        LOGGER.log(Level.FINE, "Reading HadoopManager Properties from .cfg file");
        ServiceReference<?> configurationAdminReference = context.getServiceReference(ConfigurationAdmin.class.getName());
        if (configurationAdminReference != null) {
            ConfigurationAdmin confAdmin = (ConfigurationAdmin) context.getService(configurationAdminReference);
            try {
                Configuration configuration = confAdmin.getConfiguration(HadoopManagerConstants.HADOOPMANAGER_CONFIG_FILE_NAME);
                if (configuration != null && configuration.getProperties() != null) {
                    LOGGER.log(Level.FINE, "Reading properties for HadoopManager ....");
                    Dictionary<String, Object> dict = configuration.getProperties();
                    List<String> keys = Collections.list(dict.keys());
                    Map<String, Object> dictCopy = keys.stream().collect(Collectors.toMap(Function.identity(), dict::get));
                    props = new Properties();
                    props.putAll(dictCopy);
                } else
                    LOGGER.log(Level.SEVERE, "Impossible to find Configuration admin reference, HadoopManager won't start!");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Impossible to find it.acsoftware.hyperiot.hadoopmanager.cfg, please create it!", e);
            }
        }
    }

    @Override
    public String getDefaultFS() {
        final String DEFAULT_HADOOPMANAGER_PROPERTY_DEFAULTFS = "hdfs://localhost:8020";
        return props.getProperty(HadoopManagerConstants.HADOOPMANAGER_PROPERTY_DEFAULTFS,
                DEFAULT_HADOOPMANAGER_PROPERTY_DEFAULTFS);
    }

}
