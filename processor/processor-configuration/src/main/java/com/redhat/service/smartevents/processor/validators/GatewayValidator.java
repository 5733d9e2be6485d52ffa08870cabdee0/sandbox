package com.redhat.service.smartevents.processor.validators;

import com.redhat.service.smartevents.infra.core.validations.ValidationResult;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Gateway;

public interface GatewayValidator {
    ValidationResult isValid(Gateway gateway);
}
