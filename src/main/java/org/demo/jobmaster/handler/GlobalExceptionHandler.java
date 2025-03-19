package org.demo.jobmaster.handler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Global exception handler for handling validation and application-specific exceptions.
 * <p>
 * This class centralizes exception handling for various error scenarios in the application.
 * It ensures that clients receive meaningful and structured error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handles validation errors from request bodies annotated with @Valid
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  @NotNull HttpHeaders headers,
                                                                  @NotNull HttpStatusCode status,
                                                                  @NotNull WebRequest request) {
        Map<String, String> errors = ex
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : "Unknown Error",
                        (error1, error2) -> error1 + "; " + error2
                ));

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles malformed JSON or type conversion errors in the request body
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  @NotNull HttpHeaders headers,
                                                                  @NotNull HttpStatusCode status,
                                                                  @NotNull WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        Throwable cause = ex.getCause();

        if (cause instanceof JsonParseException) {
            errors.put("error", "Invalid JSON syntax: " + cause.getMessage());
        } else if (cause instanceof UnrecognizedPropertyException unrecognizedProp) {
            String fieldName = unrecognizedProp.getPropertyName();
            errors.put(fieldName, "Unknown field: '" + fieldName + "'");
        } else if (cause instanceof InvalidFormatException formatEx) {
            String fieldPath = formatEx.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));

            String fieldName = fieldPath.isEmpty() ? "request" : fieldPath;
            Class<?> targetType = formatEx.getTargetType();
            Object value = formatEx.getValue();

            if (targetType.isEnum()) {
                String allowedValues = String.join(", ",
                        getEnumAllowedValues(targetType));

                errors.put(fieldName, String.format(
                        "Invalid value '%s'. Allowed values: [%s]",
                        value, allowedValues));
            } else {
                errors.put(fieldName, String.format(
                        "Value '%s' is not valid. Expected a %s",
                        value, getReadableTypeName(targetType)));
            }
        } else if (cause instanceof MismatchedInputException mismatchedEx) {
            if (mismatchedEx.getPath() != null && !mismatchedEx.getPath().isEmpty()) {
                String fieldPath = mismatchedEx.getPath().stream()
                        .map(JsonMappingException.Reference::getFieldName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining("."));

                errors.put(fieldPath, "Invalid format or missing value");
            } else {
                errors.put("error", "Request has an invalid format: " + mismatchedEx.getMessage());
            }
        } else {
            errors.put("error", "Invalid request body: " + ex.getMessage());
        }

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    /**
     * Utility method to get all allowed values for an enum type
     */
    private String[] getEnumAllowedValues(Class<?> enumType) {
        return java.util.Arrays.stream(enumType.getEnumConstants())
                .map(Object::toString)
                .toArray(String[]::new);
    }

    /**
     * Utility method to get a user-friendly type name
     */
    private String getReadableTypeName(Class<?> type) {
        if (Number.class.isAssignableFrom(type) ||
                type == int.class || type == long.class ||
                type == double.class || type == float.class) {
            return "number";
        }
        if (Boolean.class.isAssignableFrom(type) || type == boolean.class) {
            return "boolean (true/false)";
        }
        return type.getSimpleName().toLowerCase();
    }

    /**
     * Handles constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = ex
                .getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * Handles type mismatches in path variables or request parameters
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> errors = new HashMap<>();
        String paramName = ex.getName();
        String errorMessage = String.format(
                "Parameter '%s' should be a valid %s",
                paramName,
                ex.getRequiredType() != null ?
                        getReadableTypeName(ex.getRequiredType()) : "type");

        errors.put(paramName, errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * Fallback handler for all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected error occurred"));
    }
}