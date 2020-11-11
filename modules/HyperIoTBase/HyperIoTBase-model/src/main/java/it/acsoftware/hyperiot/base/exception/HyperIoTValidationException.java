package it.acsoftware.hyperiot.base.exception;

import java.util.Set;

import javax.validation.ConstraintViolation;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;

/**
 * @author Aristide Cittadino Model class for HyperIoTValidationException. It is
 * used to describe any constraint violation that occurs during runtime
 * exceptions.
 */
public class HyperIoTValidationException extends HyperIoTRuntimeException {
    /**
     * A unique serial version identifier
     */
    private static final long serialVersionUID = 1L;

    /**
     * Collection that contains constraint violations
     */
    private Set<ConstraintViolation<HyperIoTBaseEntity>> violations;

    /**
     * Constructor for HyperIoTValidationException
     *
     * @param violations parameter that indicates constraint violations produced
     */
    public HyperIoTValidationException(Set<ConstraintViolation<HyperIoTBaseEntity>> violations) {
        this.violations = violations;
    }

    /**
     * Gets the constraint violations
     *
     * @return Collection of constraint violations
     */
    public Set<ConstraintViolation<HyperIoTBaseEntity>> getViolations() {
        return violations;
    }

}
