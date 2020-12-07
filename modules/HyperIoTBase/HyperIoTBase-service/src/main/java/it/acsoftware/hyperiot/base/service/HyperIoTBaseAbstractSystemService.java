package it.acsoftware.hyperiot.base.service;

import it.acsoftware.hyperiot.base.api.HyperIoTResource;
import it.acsoftware.hyperiot.base.exception.HyperIoTValidationException;
import it.acsoftware.hyperiot.base.service.entity.validation.HyperIoTValidationProviderResolver;

import javax.validation.*;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HyperIoTBaseAbstractSystemService extends HyperIoTAbstractService {
    private final Logger log = Logger.getLogger(this.getClass().getName());

    /**
     * Validates bean instance
     */
    private static final Validator validator;

    static {
        Configuration<?> config = Validation.byDefaultProvider()
            .providerResolver(new HyperIoTValidationProviderResolver()).configure();

        ValidatorFactory factory = config.buildValidatorFactory();
        validator = factory.getValidator();
    }

    protected void validate(HyperIoTResource entity) {
        Set<ConstraintViolation<HyperIoTResource>> validationResults = validator.validate(entity);
        if (validationResults != null && validationResults.size() > 0) {
            log.log(Level.FINE, "System Service Validation failed for entity {0}: {1}, errors: {2}"
                , new Object[]{entity.getResourceName(), entity, validationResults});
            throw new HyperIoTValidationException(validationResults);
        }
    }


}
