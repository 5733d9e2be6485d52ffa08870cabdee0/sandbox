package com.redhat.service.smartevents.processor.validators.custom;

import java.util.Objects;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorGatewayParametersNotValidException;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.ProcessorCatalogService;
import com.redhat.service.smartevents.processor.sources.aws.AwsSqsSource;
import com.redhat.service.smartevents.processor.validators.AbstractGatewayValidator;

@ApplicationScoped
public class AwsSqsSourceValidator extends AbstractGatewayValidator implements AwsSqsSource,
        CustomGatewayValidator {

    public static final Pattern AWS_QUEUE_URL_PATTERN = Pattern.compile("^https://sqs\\.([a-z]+-[a-z]+-[0-9])\\.amazonaws\\.com/[0-9]{12}/([^/]+)$");
    public static final Pattern GENERIC_QUEUE_URL_PATTERN = Pattern.compile("^(https?://[^/]*)/([0-9]{12}/)?([a-zA-Z0-9-_]+)$");

    @Inject
    public AwsSqsSourceValidator(ProcessorCatalogService processorCatalogService) {
        super(processorCatalogService);
    }

    @Override
    public ValidationResult applyAdditionalValidations(Gateway gateway) {
        String queueUrlString = gateway.getParameter(AWS_QUEUE_URL_PARAM);
        if (Objects.isNull(queueUrlString)) {
            return ValidationResult.invalid(new ProcessorGatewayParametersNotValidException(malformedUrlMessage(queueUrlString)));
        }
        if (!AWS_QUEUE_URL_PATTERN.matcher(queueUrlString).find() && !GENERIC_QUEUE_URL_PATTERN.matcher(queueUrlString).find()) {
            return ValidationResult.invalid(new ProcessorGatewayParametersNotValidException(malformedUrlMessage(queueUrlString)));
        }

        return ValidationResult.valid();
    }

    static String malformedUrlMessage(String endpoint) {
        return String.format("Malformed \"%s\" url: \"%s\"", AWS_QUEUE_URL_PARAM, endpoint);
    }
}
