package com.redhat.service.bridge.actions;

import com.redhat.service.bridge.infra.models.actions.BaseAction;

public interface ActionParameterValidator {
    ValidationResult isValid(BaseAction baseAction);
}
