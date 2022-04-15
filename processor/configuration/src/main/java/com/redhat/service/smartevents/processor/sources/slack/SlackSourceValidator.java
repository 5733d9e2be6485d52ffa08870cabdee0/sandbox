package com.redhat.service.smartevents.processor.sources.slack;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.sources.SourceValidator;

@ApplicationScoped
public class SlackSourceValidator implements SlackSource, SourceValidator {
    public static final String INVALID_CHANNEL_MESSAGE =
            "The supplied " + CHANNEL_PARAM + " parameter is not valid";

    public static final String INVALID_TOKEN_MESSAGE =
            "The supplied " + TOKEN_PARAM + " parameter is not valid";

    @Override
    public ValidationResult isValid(Source source) {
        Map<String, String> parameters = source.getParameters();

        if (!parameters.containsKey(CHANNEL_PARAM) || parameters.get(CHANNEL_PARAM).isEmpty()) {
            return ValidationResult.invalid(INVALID_CHANNEL_MESSAGE);
        }

        if (!parameters.containsKey(TOKEN_PARAM) || parameters.get(TOKEN_PARAM).isEmpty()) {
            return ValidationResult.invalid(INVALID_TOKEN_MESSAGE);
        }

        return ValidationResult.valid();
    }
}
