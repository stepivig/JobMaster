package org.demo.jobmaster.exception;

/**
 * Exception thrown when an invalid value is provided for an enumerated type.
 */
public class InvalidEnumException extends RuntimeException {

    /**
     * Constructs a new {@code InvalidEnumException} with the specified detail message.
     *
     * @param message the detail message describing the invalid enum value
     */
    public InvalidEnumException(String message) {
        super(message);
    }
}
