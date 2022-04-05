package com.redhat.service.bridge.manager.actions.sendtobridge;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.actions.ActionParameterValidator;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.validations.ValidationResult;

@ApplicationScoped
public class SendToBridgeActionValidator implements ActionParameterValidator {

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
