package org.demo.jobmaster.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.scheduling.support.CronExpression;

/**
 * Validator for cron expressions.
 * Uses Spring's CronExpression parser to validate the format.
 */
public class CronExpressionValidator implements ConstraintValidator<ValidCronExpression, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.trim().isEmpty()) {
            return false;
        }

        try {
            return CronExpression.isValidExpression(value);
        } catch (Exception e) {
            return false;
        }
    }
}