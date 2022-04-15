package com.redhat.service.smartevents.processor.actions;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

public interface ActionValidator extends ActionBean {
    ValidationResult isValid(Action action);
}
