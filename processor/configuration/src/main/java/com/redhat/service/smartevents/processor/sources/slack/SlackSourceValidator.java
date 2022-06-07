package com.redhat.service.smartevents.processor.sources.slack;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.GatewayValidator;

@ApplicationScoped
public class SlackSourceValidator implements SlackSource, GatewayValidator<Source> {
    public static final String INVALID_CHANNEL_MESSAGE =
            "The supplied " + CHANNEL_PARAM + " parameter is not valid";

    public static final String INVALID_TOKEN_MESSAGE =
            "The supplied " + TOKEN_PARAM + " parameter is not valid";

    @Override
    public ValidationResult isValid(Source source) {
        if (!source.hasParameter(CHANNEL_PARAM) || source.getParameter(CHANNEL_PARAM).isEmpty()) {
            return ValidationResult.invalid(INVALID_CHANNEL_MESSAGE);
        }

        if (!source.hasParameter(TOKEN_PARAM) || source.getParameter(TOKEN_PARAM).isEmpty()) {
            return ValidationResult.invalid(INVALID_TOKEN_MESSAGE);
        }

        return ValidationResult.valid();
    }
}
