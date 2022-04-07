package com.redhat.service.bridge.processor.actions.slack;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.validations.ValidationResult;
import com.redhat.service.bridge.processor.actions.ActionParameterValidator;

@ApplicationScoped
public class SlackActionValidator implements SlackActionBean, ActionParameterValidator {
    public static final String INVALID_CHANNEL_MESSAGE =
            "The supplied " + SlackActionBean.CHANNEL_PARAMETER + " parameter is not valid";

    public static final String INVALID_WEBHOOK_URL_MESSAGE =
            "The supplied " + SlackActionBean.WEBHOOK_URL_PARAMETER + " parameter is not valid";

    @Override
    public ValidationResult isValid(BaseAction action) {
        Map<String, String> parameters = action.getParameters();

        if (!parameters.containsKey(SlackActionBean.CHANNEL_PARAMETER) || parameters.get(SlackActionBean.CHANNEL_PARAMETER).isEmpty()) {
            return ValidationResult.invalid(INVALID_CHANNEL_MESSAGE);
        }

        if (!parameters.containsKey(SlackActionBean.WEBHOOK_URL_PARAMETER) || parameters.get(SlackActionBean.WEBHOOK_URL_PARAMETER).isEmpty()) {
            return ValidationResult.invalid(INVALID_WEBHOOK_URL_MESSAGE);
        }

        return ValidationResult.valid();
    }
}
