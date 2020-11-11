package it.acsoftware.hyperiot.base.validators;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTAuthenticable;
import it.acsoftware.hyperiot.base.validation.ValidPassword;
import org.passay.*;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Aristide Cittadino This class implements a constraint validator, able
 * to validate password with a @ValidPassword annotation.
 */
public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, HyperIoTAuthenticable> {
    private Logger log = Logger.getLogger("it.acsoftware.hyperiot");

    @Override
    public boolean isValid(HyperIoTAuthenticable authEntity, ConstraintValidatorContext context) {
        log.log(Level.FINE, "Validating value with @ValidPassword ");
        if ((authEntity.getId() == 0
                || (authEntity.getPassword() != null && authEntity.getPasswordConfirm() != null))) {
            String password = authEntity.getPassword();
            String passwordConfirm = authEntity.getPasswordConfirm();

            if (password == null || passwordConfirm == null)
                return false;

            // reduntant but safe, because is checked in @PasswordMustmMatch
            if (!password.equals(passwordConfirm))
                return false;

            PasswordValidator validator = new PasswordValidator(Arrays.asList(

                    // at least 8 characters
                    new LengthRule(8, 30),

                    // at least one upper-case character
                    new CharacterRule(EnglishCharacterData.UpperCase, 1),

                    // at least one lower-case character
                    new CharacterRule(EnglishCharacterData.LowerCase, 1),

                    // at least one digit character
                    new CharacterRule(EnglishCharacterData.Digit, 1),

                    // at least one symbol (special character)
                    new CharacterRule(EnglishCharacterData.Special, 1),

                    // no whitespace
                    new WhitespaceRule()));

            RuleResult result = validator.validate(new PasswordData(password));

            if (result.isValid()) {
                return true;
            }

            List<String> messages = validator.getMessages(result);
            String messageTemplate = messages.stream().collect(Collectors.joining(","));
            context.buildConstraintViolationWithTemplate(messageTemplate).addConstraintViolation()
                    .disableDefaultConstraintViolation();
            log.log(Level.FINE, "@ValidPassword validation failed!");
            return false;
        }
        return true;
    }

}
