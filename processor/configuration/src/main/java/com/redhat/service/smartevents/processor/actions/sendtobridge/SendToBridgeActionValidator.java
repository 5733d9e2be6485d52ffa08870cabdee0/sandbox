package com.redhat.service.smartevents.processor.actions.sendtobridge;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.GatewayValidator;

@ApplicationScoped
public class SendToBridgeActionValidator implements SendToBridgeAction, GatewayValidator<Action> {

    public static final String INVALID_BRIDGE_ID_PARAM_MESSAGE =
            "The supplied " + BRIDGE_ID_PARAM + " parameter is not valid";

    @Override
    public ValidationResult isValid(Action action) {
        if (action.getParameters() != null) {
            return !action.hasParameter(BRIDGE_ID_PARAM) || !action.getParameter(BRIDGE_ID_PARAM).isEmpty()
                    ? ValidationResult.valid()
                    : ValidationResult.invalid(INVALID_BRIDGE_ID_PARAM_MESSAGE);
        }
        return ValidationResult.invalid();
    }
}
