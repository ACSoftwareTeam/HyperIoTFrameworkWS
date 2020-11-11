package it.acsoftware.hyperiot.base.validators;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import it.acsoftware.hyperiot.base.validation.PowOf2;

public class PowOf2Validator implements ConstraintValidator<PowOf2, Number> {
    private Logger log = Logger.getLogger("it.acsoftware.hyperiot");

    @Override
    public boolean isValid(Number value, ConstraintValidatorContext context) {
        log.log(Level.FINE, "Validating value with @PowOf2 with value: {0}" , value);
        if (value == null)
            return false;
        int intValue = value.intValue();
        boolean isValid = intValue > 0 && Math.round(value.doubleValue()) == intValue && (intValue == 1 || intValue % 2 == 0);
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("{it.acsoftware.hyperiot.validator.powOf2.message}")
                    .addConstraintViolation();
            log.log(Level.FINE, "@PowOf2 validation failed: {0}" , value);
        }
        return isValid;
    }

}
