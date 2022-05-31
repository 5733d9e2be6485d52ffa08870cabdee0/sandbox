package com.redhat.service.smartevents.processor.sources.aws;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.AbstractGatewayValidator;
import com.redhat.service.smartevents.processor.JsonSchemaService;

@ApplicationScoped
public class AwsSqsSourceValidator extends AbstractGatewayValidator<Source> implements AwsSqsSource {

    public static final Pattern AWS_QUEUE_URL_PATTERN = Pattern.compile("^https://sqs\\.([a-z]+-[a-z]+-[0-9])\\.amazonaws\\.com/[0-9]{12}/([^/]+)$");
    public static final Pattern GENERIC_QUEUE_URL_PATTERN = Pattern.compile("^(https?://[^/]*)/([0-9]{12}/)?([a-zA-Z0-9-_]+)$");

    public static final String INVALID_AWS_REGION_PARAM_MESSAGE = "Invalid \"" + AWS_REGION_PARAM + "\" parameter";

    private static final Map<String, String> EXPECTED_PARAMS = Stream.of(
            AWS_QUEUE_URL_PARAM, AWS_ACCESS_KEY_ID_PARAM, AWS_SECRET_ACCESS_KEY_PARAM)
            .collect(Collectors.toMap(k -> k, AwsSqsSourceValidator::missingParameterMessage, (k1, k2) -> k1, LinkedHashMap::new));

    @Inject
    public AwsSqsSourceValidator(JsonSchemaService jsonSchemaService) {
        super(jsonSchemaService);
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

    static String missingParameterMessage(String parameterName) {
        return "Missing or empty \"" + parameterName + "\" parameter";
    }
}
