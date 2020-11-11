package it.acsoftware.hyperiot.base.service.rest.provider;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.dosgi.common.api.IntentsProvider;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.osgi.service.component.annotations.Component;

/**
 * Author Aristide Cittadino
 */
@Component(property = { "org.apache.cxf.dosgi.IntentName=swagger" }, immediate = true)
public class HyperIoTSwaggerIntentProvider implements IntentsProvider {
	private Logger log = Logger.getLogger("it.acsoftware.hyperiot");

	@Override
	public List<?> getIntents() {
		return Arrays.asList(this.createSwaggerFeature());
	}

	private Swagger2Feature createSwaggerFeature() {
		log.log(Level.INFO, "Register HyperIoT Provider Swagger Intent");
		Swagger2Feature swagger = new Swagger2Feature();
		swagger.setUsePathBasedConfig(true); // Necessary for OSGi
        swagger.setPrettyPrint(true);
        swagger.setBasePath("/hyperiot");
        swagger.setSupportSwaggerUi(true);
		return swagger;
	}
}
