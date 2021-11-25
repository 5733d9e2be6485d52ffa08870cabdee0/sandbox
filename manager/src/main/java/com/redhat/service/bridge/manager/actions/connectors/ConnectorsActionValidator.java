package com.redhat.service.bridge.manager.actions.connectors;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.actions.ActionParameterValidator;
import com.redhat.service.bridge.actions.ValidationResult;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

@ApplicationScoped
public class ConnectorsActionValidator implements ActionParameterValidator {

    public static final String INVALID_PAYLOAD_MESSAGE =
            "The supplied " + ConnectorsAction.CONNECTOR_PAYLOAD + " parameter is not valid";

    @Override
    public ValidationResult isValid(BaseAction action) {
        if (action.getParameters() != null) {
            Map<String, String> parameters = action.getParameters();
            return !parameters.containsKey(ConnectorsAction.CONNECTOR_PAYLOAD) || !parameters.get(ConnectorsAction.CONNECTOR_PAYLOAD).isEmpty()
                    ? ValidationResult.valid()
                    : ValidationResult.invalid(INVALID_PAYLOAD_MESSAGE);
        }
        return ValidationResult.invalid();
    }
}
