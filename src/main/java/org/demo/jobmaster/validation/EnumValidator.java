package org.demo.jobmaster.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to validate that a given string matches one of the values in a specified enum.
 * This annotation ensures that the provided value corresponds to an existing enum constant.
 *
 * <p>This annotation should be used on fields or method parameters of type {@code String}.</p>
 *
 * @see EnumValidatorImpl for the actual validation logic
 */
@Constraint(validatedBy = EnumValidatorImpl.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumValidator {

    /**
     * The default error message when validation fails.
     * The message can be overridden when applying the annotation.
     *
     * @return the error message
     */
    String message() default "Invalid value. Allowed values are: {enumClass}";

    /**
     * Defines groups for validation.
     * This feature allows validation constraints to be applied in different contexts.
     *
     * @return the validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Specifies custom payloads for clients of the Jakarta Bean Validation API.
     * Typically not used in standard applications.
     *
     * @return the payload class
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * Specifies the enum class that contains the valid values.
     * The provided string value will be checked against the names of the constants in this enum.
     *
     * @return the enum class to validate against
     */
    Class<? extends Enum<?>> enumClass();
}
