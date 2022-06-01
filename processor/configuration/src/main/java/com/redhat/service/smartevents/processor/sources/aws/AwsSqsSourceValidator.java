package com.redhat.service.smartevents.processor.sources.aws;

import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.AbstractGatewayValidator;
import com.redhat.service.smartevents.processor.ProcessorCatalogService;

@ApplicationScoped
public class AwsSqsSourceValidator extends AbstractGatewayValidator<Source> implements AwsSqsSource {

    public static final Pattern AWS_QUEUE_URL_PATTERN = Pattern.compile("^https://sqs\\.([a-z]+-[a-z]+-[0-9])\\.amazonaws\\.com/[0-9]{12}/([^/]+)$");
    public static final Pattern GENERIC_QUEUE_URL_PATTERN = Pattern.compile("^(https?://[^/]*)/([0-9]{12}/)?([a-zA-Z0-9-_]+)$");

    @Inject
    public AwsSqsSourceValidator(ProcessorCatalogService processorCatalogService) {
        super(processorCatalogService);
    }

    @Override
    public ValidationResult applyAdditionalValidations(Source source) {
        String queueUrlString = source.getParameter(AWS_QUEUE_URL_PARAM);
        if (!AWS_QUEUE_URL_PATTERN.matcher(queueUrlString).find() && !GENERIC_QUEUE_URL_PATTERN.matcher(queueUrlString).find()) {
            return ValidationResult.invalid(malformedUrlMessage(queueUrlString));
        }

        return ValidationResult.valid();
    }

    static String malformedUrlMessage(String endpoint) {
        return String.format("Malformed \"%s\" url: \"%s\"", AWS_QUEUE_URL_PARAM, endpoint);
    }
}
