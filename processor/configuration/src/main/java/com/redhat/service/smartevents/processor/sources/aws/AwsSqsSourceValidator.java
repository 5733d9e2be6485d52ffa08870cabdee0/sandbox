package com.redhat.service.smartevents.processor.sources.aws;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.GatewayValidator;

@ApplicationScoped
public class AwsSqsSourceValidator implements AwsSqsSource, GatewayValidator<Source> {
    public static final String INVALID_AWS_QUEUE_URL_MESSAGE =
            "The supplied " + AWS_QUEUE_URL_PARAM + " parameter is missing or not valid";

    @Override
    public ValidationResult isValid(Source source) {
        Map<String, String> parameters = source.getParameters();

        if (!parameters.containsKey(AWS_QUEUE_URL_PARAM) || parameters.get(AWS_QUEUE_URL_PARAM).isEmpty()) {
            return ValidationResult.invalid(INVALID_AWS_QUEUE_URL_MESSAGE);
        }

        return ValidationResult.valid();
    }
}
