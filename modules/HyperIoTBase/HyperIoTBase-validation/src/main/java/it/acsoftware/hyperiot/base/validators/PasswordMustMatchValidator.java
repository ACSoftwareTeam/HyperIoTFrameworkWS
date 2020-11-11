package it.acsoftware.hyperiot.base.validators;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTAuthenticable;
import it.acsoftware.hyperiot.base.validation.PasswordMustMatch;

/**
 * @author Aristide Cittadino This class implements a constraint validator, able
 * to validate password with a @PasswordMustMatch annotation.
 */
public class PasswordMustMatchValidator implements ConstraintValidator<PasswordMustMatch, HyperIoTAuthenticable> {
    private Logger log = Logger.getLogger("it.acsoftware.hyperiot");

    @Override
    public boolean isValid(HyperIoTAuthenticable authEntity, ConstraintValidatorContext context) {
        log.log(Level.FINE,
                "Validating value with @PasswordMustMatch with authEntity: {0}" , authEntity.getClass().getName());
        if ((authEntity.getId() == 0 || (authEntity.getPassword() != null && authEntity.getPasswordConfirm() != null))
                && (authEntity.getPassword() == null
                || !authEntity.getPassword().equals(authEntity.getPasswordConfirm()))) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}").addConstraintViolation();
            log.log(Level.FINE, "@PasswordMustMatch validation failed: {0}" , authEntity.getClass().getName());
            return false;
        }
        return true;
    }

}
