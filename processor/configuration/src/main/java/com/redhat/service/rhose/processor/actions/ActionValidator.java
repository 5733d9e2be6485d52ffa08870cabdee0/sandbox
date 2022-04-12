package com.redhat.service.rhose.processor.actions;

import com.redhat.service.rhose.infra.models.actions.BaseAction;
import com.redhat.service.rhose.infra.validations.ValidationResult;

public interface ActionValidator extends ActionBean {
    ValidationResult isValid(BaseAction baseAction);
}
