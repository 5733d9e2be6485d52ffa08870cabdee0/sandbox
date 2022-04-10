package com.redhat.service.bridge.processor.actions.slack;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.validations.ValidationResult;
import com.redhat.service.bridge.processor.actions.ActionValidator;

@ApplicationScoped
public class SlackActionValidator implements SlackActionBean, ActionValidator {
    public static final String INVALID_CHANNEL_MESSAGE =
            "The supplied " + SlackAction.CHANNEL_PARAM + " parameter is not valid";

    public static final String INVALID_WEBHOOK_URL_MESSAGE =
            "The supplied " + SlackAction.WEBHOOK_URL_PARAM + " parameter is not valid";

    @Override
    public ValidationResult isValid(BaseAction action) {
        Map<String, String> parameters = action.getParameters();

        if (!parameters.containsKey(SlackAction.CHANNEL_PARAM) || parameters.get(SlackAction.CHANNEL_PARAM).isEmpty()) {
            return ValidationResult.invalid(INVALID_CHANNEL_MESSAGE);
        }

        if (!parameters.containsKey(SlackAction.WEBHOOK_URL_PARAM) || parameters.get(SlackAction.WEBHOOK_URL_PARAM).isEmpty()) {
            return ValidationResult.invalid(INVALID_WEBHOOK_URL_MESSAGE);
        }

        return ValidationResult.valid();
    }
}
