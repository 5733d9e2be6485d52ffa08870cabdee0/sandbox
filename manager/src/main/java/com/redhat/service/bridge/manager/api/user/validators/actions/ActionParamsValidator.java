package com.redhat.service.bridge.manager.api.user.validators.actions;

import com.redhat.service.bridge.infra.models.actions.ActionRequest;

public interface ActionParamsValidator {

    boolean accepts(ActionRequest actionRequest);

    boolean isValid(ActionRequest actionRequest);
}
