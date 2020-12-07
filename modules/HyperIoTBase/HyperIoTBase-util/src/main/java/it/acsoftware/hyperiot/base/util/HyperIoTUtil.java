package it.acsoftware.hyperiot.base.util;

import it.acsoftware.hyperiot.base.api.HyperIoTPostAction;
import it.acsoftware.hyperiot.base.api.HyperIoTResource;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTSharedEntity;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilter;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import org.apache.commons.lang3.ClassUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Aristide Cittadino Model class for HyperIoTUtil. It is a utility
 * class and implements the method to obtain a password encoded with the
 * MD5 algorithm.
 */
public final class HyperIoTUtil {
    private static Logger log = Logger.getLogger(HyperIoTUtil.class.getName());
    private static Properties props;
    private static HyperIoTUtil localInstance;

    private BundleContext bundleContext;

    public static Properties getHyperIoTProperties(BundleContext context) {
        if (props == null) {
            ServiceReference<?> configurationAdminReference = context
                .getServiceReference(ConfigurationAdmin.class.getName());

            if (configurationAdminReference != null) {
                ConfigurationAdmin confAdmin = (ConfigurationAdmin) context
                    .getService(configurationAdminReference);
                try {
                    Configuration configuration = confAdmin
                        .getConfiguration("it.acsoftware.hyperiot");
                    if (configuration != null && configuration.getProperties() != null) {
                        Dictionary<String, Object> dict = configuration.getProperties();
                        List<String> keys = Collections.list(dict.keys());
                        Map<String, Object> dictCopy = keys.stream()
                            .collect(Collectors.toMap(Function.identity(), dict::get));
                        props = new Properties();
                        props.putAll(dictCopy);
                        log.log(Level.FINER, "Loaded properties For HyperIoT: {0}", props);
                        return props;
                    }
                } catch (IOException e) {
                    log.log(Level.SEVERE,
                        "Impossible to find it.acsoftware.hyperiot.cfg, please create it!", e);
                    return null;
                }
            }
            log.log(Level.SEVERE,
                "Impossible to find it.acsoftware.hyperiot.cfg, please create it!");
            return null;
        }
        return props;
    }

    public static Object getHyperIoTProperty(String name) {
        return getHyperIoTProperties(getBundleContext(HyperIoTUtil.class)).getProperty(name);
    }

    public static Object getHyperIoTProperty(String name, String defaultValue) {
        return getHyperIoTProperties(getBundleContext(HyperIoTUtil.class)).getProperty(name, defaultValue);
    }

    /**
     * @return true if test mode is enabled inside the it.acsoftware.hyperiot.cfg
     * file
     */
    public static boolean isInTestMode() {
        if (props == null)
            HyperIoTUtil.getHyperIoTProperties(getBundleContext(HyperIoTUtil.class));

        return Boolean.parseBoolean(
            props.getProperty(HyperIoTConstants.HYPERIOT_PROPERTY_TEST_MODE, "false"));
    }

    /**
     * @return nodeID of this current node, if property is not set returns a random
     * UUID
     */
    public static String getNodeId() {
        if (props == null)
            HyperIoTUtil.getHyperIoTProperties(getBundleContext(HyperIoTUtil.class));

        return props.getProperty(HyperIoTConstants.HYPERIOT_PROPERTY_NODE_ID,
            UUID.randomUUID().toString());
    }

    /**
     * @return layer of this current node, if property is not set returns
     * "undefined_layer"
     */
    public static String getLayer() {
        if (props == null)
            HyperIoTUtil.getHyperIoTProperties(getBundleContext(HyperIoTUtil.class));

        return props.getProperty(HyperIoTConstants.HYPERIOT_PROPERTY_LAYER, "undefined_layer");
    }

    /**
     * Return password encoded with the MD5 algorithm
     *
     * @param password parameter that indicates the password to access the platform
     * @return Password encoded
     */
    public static String getPasswordHash(String password) {
        try {
            log.log(Level.FINE, "Hashing password....");
            return Base64.getEncoder()
                .encodeToString(MessageDigest.getInstance("MD5").digest(password.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            return Base64.getEncoder().encodeToString(password.getBytes());
        }
    }

    /**
     * @return layer of this current node, if property is not set returns
     * "localhost:8080"
     */
    public static String getHyperIoTUrl() {
        if (props == null)
            HyperIoTUtil.getHyperIoTProperties(getBundleContext(HyperIoTUtil.class));

        return props.getProperty(HyperIoTConstants.HYPERIOT_PROPERTY_SERVICES_URL, "localhost:8181");
    }

    /**
     * @return
     */
    public static String getActivateAccountUrl() {
        if (props == null)
            HyperIoTUtil.getHyperIoTProperties(getBundleContext(HyperIoTUtil.class));

        return props.getProperty(HyperIoTConstants.HYPERIOT_PROPERTY_ACTIVATE_ACCOUNT_URL, "localhost:4200/auth/activation");
    }

    /**
     * @return
     */
    public static String getFrontEndUrl() {
        if (props == null)
            HyperIoTUtil.getHyperIoTProperties(getBundleContext(HyperIoTUtil.class));

        return props.getProperty(HyperIoTConstants.HYPERIOT_PROPERTY_FRONTEND_URL, "localhost:4200");
    }

    /**
     * @return
     */
    public static String getPasswordResetUrl() {
        if (props == null)
            HyperIoTUtil.getHyperIoTProperties(getBundleContext(HyperIoTUtil.class));

        return props.getProperty(HyperIoTConstants.HYPERIOT_PROPERTY_PASSWORD_RESET_URL, "localhost:4200/auth/password-reset");
    }

    public static int getWebSocketOnOpenDispatchThreads(int defaultThreadNumber) {
        if (props == null)
            HyperIoTUtil.getHyperIoTProperties(getBundleContext(HyperIoTUtil.class));
        return Integer.parseInt(props.getProperty(HyperIoTConstants.HYPERIOT_PROPERTY_WEB_SOCKER_SERVICE_ON_OPEN_DISPATCH_THREADS, String.valueOf(defaultThreadNumber)));
    }

    public static int getWebSocketOnCloseDispatchThreads(int defaultThreadNumber) {
        if (props == null)
            HyperIoTUtil.getHyperIoTProperties(getBundleContext(HyperIoTUtil.class));
        return Integer.parseInt(props.getProperty(HyperIoTConstants.HYPERIOT_PROPERTY_WEB_SOCKER_SERVICE_ON_CLOSE_DISPATCH_THREADS, String.valueOf(defaultThreadNumber)));
    }

    public static int getWebSocketOnMessageDispatchThreads(int defaultThreadNumber) {
        if (props == null)
            HyperIoTUtil.getHyperIoTProperties(getBundleContext(HyperIoTUtil.class));
        return Integer.parseInt(props.getProperty(HyperIoTConstants.HYPERIOT_PROPERTY_WEB_SOCKER_SERVICE_ON_MESSAGE_DISPATCH_THREADS, String.valueOf(defaultThreadNumber)));
    }

    public static int getWebSocketOnErrorDispatchThreads(int defaultThreadNumber) {
        if (props == null)
            HyperIoTUtil.getHyperIoTProperties(getBundleContext(HyperIoTUtil.class));
        return Integer.parseInt(props.getProperty(HyperIoTConstants.HYPERIOT_PROPERTY_WEB_SOCKER_SERVICE_ON_ERROR_DISPATCH_THREADS, String.valueOf(defaultThreadNumber)));
    }

    /**
     * @return base rest context of rest services, default is: /hyperiot
     */
    public static String getHyperIoTBaseRestContext(BundleContext context) {
        if (props == null)
            HyperIoTUtil.getHyperIoTProperties(context);
        String baseRestUrl = props.getProperty(HyperIoTConstants.HYPERIOT_PROPERTY_BASE_REST_CONTEXT, "/hyperiot");
        if (!baseRestUrl.startsWith("/"))
            baseRestUrl = "/" + baseRestUrl;
        return baseRestUrl;
    }


    /**
     * Returns the BundleContext of the bundle which contains this component.
     *
     * @param instance parameter that indicates the instance contained by the bundle
     * @return The BundleContext of the bundle
     */
    public static BundleContext getBundleContext(Object instance) {
        BundleContext bundleContext = FrameworkUtil.getBundle(instance.getClass())
            .getBundleContext();
        return bundleContext;
    }

    /**
     * Returns the BundleContext of the bundle which contains this component.
     *
     * @param className parameter that indicates the class name of the bundle
     * @return The BundleContext of the bundle
     */
    public static BundleContext getBundleContext(Class<?> className) {
        BundleContext bundleContext = FrameworkUtil.getBundle(className).getBundleContext();
        return bundleContext;
    }

    /**
     * @param c Class or Interface of the needed service
     * @return Service instance
     */
    public static Object getService(Class<?> c) {
        BundleContext ctx = getBundleContext(c);
        ServiceReference<?> sr = ctx.getServiceReference(c);
        return ctx.getService(sr);
    }

    /**
     * @param c      Class
     * @param filter OSGi filter
     * @return array of references
     */
    public static ServiceReference[] getServices(Class<?> c, String filter) {
        BundleContext ctx = getBundleContext(c);
        ServiceReference<?> sr = ctx.getServiceReference(c);
        try {
            return ctx.getServiceReferences(c.getName(), filter);
        } catch (InvalidSyntaxException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return new ServiceReference[]{};
    }

    /**
     * Execute post actions after another one (save, update or delete on entities)
     *
     * @param resource   Entity on which the first action was executed
     * @param postAction HyperIoTPostAction Post action instance
     */
    @SuppressWarnings("unchecked")
    public static void invokePostActions(HyperIoTResource resource, Class<? extends HyperIoTPostAction> postAction) {
        log.log(Level.FINE, "Fetch post actions of type: {0}", postAction);

        OSGiFilter osgiFilter = OSGiFilterBuilder.createFilter("type", resource.getClass().getName());

        //include OSGi filters for all interfaces implemented by resource and its superclasses
        String filter = ClassUtils.getAllInterfaces(resource.getClass())
                .stream()
                .map(interfaceClass -> OSGiFilterBuilder.createFilter("type", interfaceClass.getName()))
                .reduce(osgiFilter, OSGiFilter::or)
                .getFilter();

        ServiceReference<? extends HyperIoTPostAction>[] serviceReferences =
            HyperIoTUtil.getServices(postAction, filter);
        if (serviceReferences == null)
            log.log(Level.FINE, "There are not post actions of type {0}", postAction);
        else {
            log.log(Level.FINE, "{0} post actions fetched", serviceReferences.length);
            for (ServiceReference<? extends HyperIoTPostAction> serviceReference : serviceReferences)
                try {
                    log.log(Level.FINE, "Executing post action: {0}", serviceReference);
                    HyperIoTPostAction hyperIoTPostAction = HyperIoTUtil.getBundleContext(postAction).getService(serviceReference);
                    hyperIoTPostAction.execute(resource);
                } catch (Throwable e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
        }
    }

}
