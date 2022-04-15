package com.redhat.service.smartevents.processor.actions.slack;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.actions.ActionValidator;

@ApplicationScoped
public class SlackActionValidator implements SlackAction, ActionValidator {
    public static final String INVALID_CHANNEL_MESSAGE =
            "The supplied " + CHANNEL_PARAM + " parameter is not valid";

    public static final String INVALID_WEBHOOK_URL_MESSAGE =
            "The supplied " + WEBHOOK_URL_PARAM + " parameter is not valid";

    @Override
    public ValidationResult isValid(Action action) {
        Map<String, String> parameters = action.getParameters();

        if (!parameters.containsKey(CHANNEL_PARAM) || parameters.get(CHANNEL_PARAM).isEmpty()) {
            return ValidationResult.invalid(INVALID_CHANNEL_MESSAGE);
        }

        if (!parameters.containsKey(WEBHOOK_URL_PARAM) || parameters.get(WEBHOOK_URL_PARAM).isEmpty()) {
            return ValidationResult.invalid(INVALID_WEBHOOK_URL_MESSAGE);
        }

        return ValidationResult.valid();
    }
}
