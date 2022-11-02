package com.redhat.service.smartevents.processor.validators;

import com.redhat.service.smartevents.infra.core.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.core.validations.ValidationResult;

public interface GatewayValidator {
    ValidationResult isValid(Gateway gateway);
}
