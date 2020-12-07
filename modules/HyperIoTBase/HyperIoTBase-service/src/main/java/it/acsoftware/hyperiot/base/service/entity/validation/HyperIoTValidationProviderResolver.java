package it.acsoftware.hyperiot.base.service.entity.validation;

import org.hibernate.validator.HibernateValidator;

import javax.validation.ValidationProviderResolver;
import javax.validation.spi.ValidationProvider;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Aristide Cittadino Model class for
 * HyperIoTValidationProviderResolver. This class implements
 * ValidationProviderResolver interface in order to validate a list of
 * data from hibernate side.
 */
public class HyperIoTValidationProviderResolver implements ValidationProviderResolver {
    protected Logger log = Logger.getLogger(HyperIoTValidationProviderResolver.class.getName());

    @Override
    public List<ValidationProvider<?>> getValidationProviders() {
        log.log(Level.FINER, "Returning Validation provider HibernateValidator");
        return Collections.singletonList((new HibernateValidator()));
    }

}
