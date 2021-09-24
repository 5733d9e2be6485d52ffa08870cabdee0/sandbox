package com.redhat.service.bridge.actions;

public class ValidationResult {

    private final boolean valid;

    private final String message;

    public ValidationResult(boolean valid) {
        this(valid, null);
    }

    public ValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public static ValidationResult valid() {
        return new ValidationResult(true);
    }

    public static ValidationResult invalid() {
        return invalid(null);
    }

    public static ValidationResult invalid(String message) {
        return new ValidationResult(false, message);
    }
}
