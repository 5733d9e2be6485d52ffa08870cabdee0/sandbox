package com.redhat.service.bridge.processor.actions.slack;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.validations.ValidationResult;
import com.redhat.service.bridge.processor.actions.ActionValidator;

@ApplicationScoped
public class SlackActionValidator implements SlackActionBean, ActionValidator {
    public static final String INVALID_CHANNEL_MESSAGE =
            "The supplied " + SlackActionBean.CHANNEL_PARAM + " parameter is not valid";

    public static final String INVALID_WEBHOOK_URL_MESSAGE =
            "The supplied " + SlackActionBean.WEBHOOK_URL_PARAM + " parameter is not valid";

    @Override
    public ValidationResult isValid(BaseAction action) {
        Map<String, String> parameters = action.getParameters();

        if (!parameters.containsKey(SlackActionBean.CHANNEL_PARAM) || parameters.get(SlackActionBean.CHANNEL_PARAM).isEmpty()) {
            return ValidationResult.invalid(INVALID_CHANNEL_MESSAGE);
        }

        if (!parameters.containsKey(SlackActionBean.WEBHOOK_URL_PARAM) || parameters.get(SlackActionBean.WEBHOOK_URL_PARAM).isEmpty()) {
            return ValidationResult.invalid(INVALID_WEBHOOK_URL_MESSAGE);
        }

        return ValidationResult.valid();
    }
}
