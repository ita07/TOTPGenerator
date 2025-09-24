package io.github.ita07.model;

/**
 * Represents the result of a validation operation
 */
public class ValidationResult {
    private final boolean valid;
    private final String errorMessage;

    private ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    /**
     * Creates a successful validation result
     */
    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }

    /**
     * Creates a failed validation result with an error message
     */
    public static ValidationResult error(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }

    /**
     * Returns true if validation passed
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns the error message if validation failed, null if successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns true if validation failed
     */
    public boolean hasError() {
        return !valid;
    }
}