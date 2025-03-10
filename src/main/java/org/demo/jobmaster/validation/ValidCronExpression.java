package org.demo.jobmaster.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a string is a valid cron expression.
 * The annotated field can be null (for non-CRON schedule types).
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CronExpressionValidator.class)
@Documented
public @interface ValidCronExpression {
    String message() default "Invalid cron expression";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}