package com.redhat.service.smartevents.infra.core.validations;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {

    private final List<Violation> violations;

    public static class Violation {

        private Exception exception;

        public Violation(Exception exception) {
            this.exception = exception;
        }

        public Exception getException() {
            return exception;
        }
    }

    ValidationResult() {
        this(new ArrayList<>());
    }

    ValidationResult(List<Violation> violations) {
        this.violations = violations;
    }

    public boolean isValid() {
        return this.violations.isEmpty();
    }

    public List<Violation> getViolations() {
        return violations;
    }

    public static ValidationResult valid() {
        return new ValidationResult();
    }

    public static ValidationResult invalid(Exception exception) {
        return invalid(List.of(new Violation(exception)));
    }

    public static ValidationResult invalid(List<Violation> violations) {
        return new ValidationResult(violations);
    }
}
