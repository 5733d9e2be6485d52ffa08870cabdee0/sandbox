package com.redhat.service.bridge.processor.actions.common;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.validations.ValidationResult;

public interface ActionParameterValidator extends ActionAccepter {
    ValidationResult isValid(BaseAction baseAction);
}
