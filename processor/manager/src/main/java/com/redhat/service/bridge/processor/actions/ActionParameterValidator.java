package com.redhat.service.bridge.processor.actions;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.validations.ValidationResult;

public interface ActionParameterValidator extends ActionBean {
    ValidationResult isValid(BaseAction baseAction);
}
