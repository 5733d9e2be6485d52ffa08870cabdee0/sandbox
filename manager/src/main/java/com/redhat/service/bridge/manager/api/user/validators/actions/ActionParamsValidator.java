package com.redhat.service.bridge.manager.api.user.validators.actions;

import com.redhat.service.bridge.infra.models.actions.BaseAction;

public interface ActionParamsValidator {

    boolean accepts(BaseAction baseAction);

    boolean isValid(BaseAction baseAction);
}
