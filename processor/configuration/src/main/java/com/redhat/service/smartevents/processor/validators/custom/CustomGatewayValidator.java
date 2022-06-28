package com.redhat.service.smartevents.processor.validators.custom;

import com.redhat.service.smartevents.processor.GatewayBean;
import com.redhat.service.smartevents.processor.validators.GatewayValidator;

/**
 * for additional checks, implement this class like
 *
 * <pre>
 *
 * &#64;ApplicationScoped
 * public class AwsSqsSourceValidator extends AbstractGatewayValidator implements AwsSqsSource, CustomGatewayValidator {
 *
 *     public static final Pattern AWS_QUEUE_URL_PATTERN = Pattern.compile("^https://sqs\\.([a-z]+-[a-z]+-[0-9])\\.amazonaws\\.com/[0-9]{12}/([^/]+)$");
 *     public static final Pattern GENERIC_QUEUE_URL_PATTERN = Pattern.compile("^(https?://[^/]*)/([0-9]{12}/)?([a-zA-Z0-9-_]+)$");
 *
 *     &#64;Inject
 *     public AwsSqsSourceValidator(ProcessorCatalogService processorCatalogService) {
 *         super(processorCatalogService);
 *     }
 *
 *     &#64;Override
 *     public ValidationResult applyAdditionalValidations(Gateway gateway) {
 *         String queueUrlString = gateway.getParameter(AWS_QUEUE_URL_PARAM);
 *         if (!AWS_QUEUE_URL_PATTERN.matcher(queueUrlString).find() && !GENERIC_QUEUE_URL_PATTERN.matcher(queueUrlString).find()) {
 *             return ValidationResult.invalid(malformedUrlMessage(queueUrlString));
 *         }
 *
 *         return ValidationResult.valid();
 *     }
 *
 *     static String malformedUrlMessage(String endpoint) {
 *         return String.format("Malformed \"%s\" url: \"%s\"", AWS_QUEUE_URL_PARAM, endpoint);
 *     }
 * }
 *
 * </pre>
 */
public interface CustomGatewayValidator extends GatewayBean,
        GatewayValidator {
}