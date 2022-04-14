package com.redhat.service.smartevents.processor.actions;

import com.redhat.service.smartevents.infra.models.actions.BaseAction;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

public interface ActionValidator extends ActionBean {
    ValidationResult isValid(BaseAction baseAction);
}
