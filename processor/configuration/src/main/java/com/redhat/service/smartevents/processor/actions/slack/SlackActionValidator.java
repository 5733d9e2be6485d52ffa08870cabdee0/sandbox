package com.redhat.service.smartevents.processor.actions.slack;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.GatewayValidator;

@ApplicationScoped
public class SlackActionValidator implements SlackAction, GatewayValidator<Action> {
    public static final String INVALID_CHANNEL_MESSAGE =
            "The supplied " + CHANNEL_PARAM + " parameter is not valid";

    public static final String INVALID_WEBHOOK_URL_MESSAGE =
            "The supplied " + WEBHOOK_URL_PARAM + " parameter is not valid";

    @Override
    public ValidationResult isValid(Action action) {
        if (!action.hasParameter(CHANNEL_PARAM) || action.getParameter(CHANNEL_PARAM).isEmpty()) {
            return ValidationResult.invalid(INVALID_CHANNEL_MESSAGE);
        }

        if (!action.hasParameter(WEBHOOK_URL_PARAM) || action.getParameter(WEBHOOK_URL_PARAM).isEmpty()) {
            return ValidationResult.invalid(INVALID_WEBHOOK_URL_MESSAGE);
        }

        return ValidationResult.valid();
    }
}
