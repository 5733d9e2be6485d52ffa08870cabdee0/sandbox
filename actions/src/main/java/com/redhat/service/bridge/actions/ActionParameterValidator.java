package com.redhat.service.bridge.actions;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.validations.ValidationResult;

public interface ActionParameterValidator {
    ValidationResult isValid(BaseAction baseAction);
}
