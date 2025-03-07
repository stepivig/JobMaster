package org.demo.jobmaster.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

/**
 * Validator implementation for the {@link EnumValidator} annotation.
 * This class checks whether a given string value matches any of the constants in a specified enum.
 *
 * @see EnumValidator for the annotation definition
 */
public class EnumValidatorImpl implements ConstraintValidator<EnumValidator, String> {

    /**
     * The enum class against which the value will be validated.
     */
    private Class<? extends Enum<?>> enumClass;

    /**
     * Initializes the validator with the enum class specified in the {@link EnumValidator} annotation.
     *
     * @param constraintAnnotation the annotation instance containing the enum class
     */
    @Override
    public void initialize(EnumValidator constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
    }

    /**
     * Checks if the given string value is a valid constant in the specified enum.
     *
     * @param value   the input value to validate
     * @param context the constraint validation context (not used in this implementation)
     * @return {@code true} if the value is a valid enum constant or null, {@code false} otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .anyMatch(enumValue -> enumValue.equals(value));
    }
}