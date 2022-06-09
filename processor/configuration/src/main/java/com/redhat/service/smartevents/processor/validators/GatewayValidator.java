package com.redhat.service.smartevents.processor.validators;

import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

public interface GatewayValidator {
    ValidationResult isValid(Gateway gateway);
}
