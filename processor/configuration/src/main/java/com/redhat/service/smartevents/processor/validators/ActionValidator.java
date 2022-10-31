package com.redhat.service.smartevents.processor.validators;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

public interface ActionValidator {
    ValidationResult isValid(Action action);
}
