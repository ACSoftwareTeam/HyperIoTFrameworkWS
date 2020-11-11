package it.acsoftware.hyperiot.base.service.rest.provider;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.dosgi.common.api.IntentsProvider;
import org.osgi.service.component.annotations.Component;

import it.acsoftware.hyperiot.base.exception.GenericExceptionMapperProvider;

/**
 * Author Generoso Martello
 */
@Component(property = {"org.apache.cxf.dosgi.IntentName=exceptionmapper"}, immediate = true)
public class HyperIoTExceptionMapperProvider implements IntentsProvider {
    private Logger log = Logger.getLogger("it.acsoftware.hyperiot");

    @Override
    public List<?> getIntents() {
        log.log(Level.INFO, "Register HyperIoT Provider ExceptionMapper Intent");
        return Arrays.asList(new GenericExceptionMapperProvider());
    }

}
