package com.redhat.service.bridge.processor.actions.sendtobridge;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.validations.ValidationResult;
import com.redhat.service.bridge.processor.actions.ActionValidator;

@ApplicationScoped
public class SendToBridgeActionValidator implements SendToBridgeAction, ActionValidator {

    public static final String INVALID_BRIDGE_ID_PARAM_MESSAGE =
            "The supplied " + SendToBridgeAction.BRIDGE_ID_PARAM + " parameter is not valid";

    @Override
    public ValidationResult isValid(BaseAction action) {
        if (action.getParameters() != null) {
            Map<String, String> parameters = action.getParameters();
            return !parameters.containsKey(SendToBridgeAction.BRIDGE_ID_PARAM) || !parameters.get(SendToBridgeAction.BRIDGE_ID_PARAM).isEmpty()
                    ? ValidationResult.valid()
                    : ValidationResult.invalid(INVALID_BRIDGE_ID_PARAM_MESSAGE);
        }
        return ValidationResult.invalid();
    }
}
