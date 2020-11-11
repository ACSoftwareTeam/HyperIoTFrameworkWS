package it.acsoftware.hyperiot.base.service.rest;

import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HyperIoTBaseRestActivator implements BundleActivator {
    private Logger logger = Logger.getLogger("it.acsoftware.hyperiot");
    private HyperIoTBaseRestService instance;

    @Override
    public void start(BundleContext context) throws Exception {
        logger.log(Level.INFO, "Registering base REST resources at: {0}" , HyperIoTUtil.getHyperIoTBaseRestContext(context));
        //Registering manually in order to overidde base path
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("service.exported.interfaces", "it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestService");
        props.put("service.exported.configs", "org.apache.cxf.rs");
        props.put("org.apache.cxf.rs.address", "/hbase");
        props.put("org.apache.cxf.rs.httpservice.context", HyperIoTUtil.getHyperIoTBaseRestContext(context));
        props.put("service.exported.intents", "jackson");
        props.put("service.exported.intents", "jwtAuthFilter");
        instance = new HyperIoTBaseRestService();
        context.registerService(HyperIoTBaseRestService.class.getName(), instance, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {

    }
}
