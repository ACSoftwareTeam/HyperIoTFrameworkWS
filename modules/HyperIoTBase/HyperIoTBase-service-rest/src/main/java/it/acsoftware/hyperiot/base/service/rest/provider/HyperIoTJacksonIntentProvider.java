package it.acsoftware.hyperiot.base.service.rest.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import org.apache.cxf.dosgi.common.api.IntentsProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Author Aristide Cittadino.
 */
@Component(property = {"org.apache.cxf.dosgi.IntentName=jackson"}, immediate = true)
public class HyperIoTJacksonIntentProvider implements IntentsProvider {
    private static Logger log = Logger.getLogger("it.acsoftware.hyperiot");
    private JacksonJsonProvider jsonProvider;
    private ObjectMapper mapper;

    public HyperIoTJacksonIntentProvider(){
        mapper = new ObjectMapper();
        this.jsonProvider = new JacksonJsonProvider(mapper);
    }

    @Override
    public List<?> getIntents() {
        log.log(Level.INFO, "Register HyperIoT Provider Jackson Intent");
        return Arrays.asList(jsonProvider);
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}
