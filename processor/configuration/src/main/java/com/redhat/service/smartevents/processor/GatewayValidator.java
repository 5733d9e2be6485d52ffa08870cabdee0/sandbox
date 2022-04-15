package com.redhat.service.smartevents.processor;

import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

public interface GatewayValidator<T extends Gateway> extends GatewayBean<T> {
    ValidationResult isValid(T gateway);
}
