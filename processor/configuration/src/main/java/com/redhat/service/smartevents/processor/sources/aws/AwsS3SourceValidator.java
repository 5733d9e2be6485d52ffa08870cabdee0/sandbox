package com.redhat.service.smartevents.processor.sources.aws;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.GatewayValidator;

import static com.redhat.service.smartevents.processor.ValidatorUtils.notValidKey;

@ApplicationScoped
public class AwsS3SourceValidator implements AwsS3Source, GatewayValidator<Source> {
    public static final String INVALID_BUCKET_NAME_OR_ARN_PARAMETER_MESSAGE = notValidKey(BUCKET_NAME_OR_ARN_PARAMETER);
    public static final String INVALID_REGION_PARAMETER_MESSAGE = notValidKey(REGION_PARAMETER);
    public static final String INVALID_ACCESS_KEY_PARAMETER_MESSAGE = notValidKey(ACCESS_KEY_PARAMETER);
    public static final String INVALID_SECRET_KEY_PARAMETER_MESSAGE = notValidKey(SECRET_KEY_PARAMETER);
    public static final String INVALID_IGNORE_BODY_PARAMETER_MESSAGE = notValidKey(IGNORE_BODY_PARAMETER);
    public static final String INVALID_DELETE_AFTER_READ_PARAMETER_MESSAGE = notValidKey(DELETE_AFTER_READ_PARAMETER);

    @Override
    public ValidationResult isValid(Source source) {
        Map<String, String> parameters = source.getParameters();

        if (!parameters.containsKey(BUCKET_NAME_OR_ARN_PARAMETER) || parameters.get(BUCKET_NAME_OR_ARN_PARAMETER).isEmpty()) {
            return ValidationResult.invalid(INVALID_BUCKET_NAME_OR_ARN_PARAMETER_MESSAGE);
        }

        if (!parameters.containsKey(REGION_PARAMETER) || parameters.get(REGION_PARAMETER).isEmpty()) {
            return ValidationResult.invalid(INVALID_REGION_PARAMETER_MESSAGE);
        }

        if (!parameters.containsKey(ACCESS_KEY_PARAMETER) || parameters.get(ACCESS_KEY_PARAMETER).isEmpty()) {
            return ValidationResult.invalid(INVALID_ACCESS_KEY_PARAMETER_MESSAGE);
        }

        if (!parameters.containsKey(SECRET_KEY_PARAMETER) || parameters.get(SECRET_KEY_PARAMETER).isEmpty()) {
            return ValidationResult.invalid(INVALID_SECRET_KEY_PARAMETER_MESSAGE);
        }

        if (!parameters.containsKey(IGNORE_BODY_PARAMETER) || parameters.get(IGNORE_BODY_PARAMETER).isEmpty()) {
            return ValidationResult.invalid(INVALID_IGNORE_BODY_PARAMETER_MESSAGE);
        }

        if (!parameters.containsKey(DELETE_AFTER_READ_PARAMETER) || parameters.get(DELETE_AFTER_READ_PARAMETER).isEmpty()) {
            return ValidationResult.invalid(INVALID_DELETE_AFTER_READ_PARAMETER_MESSAGE);
        }

        return ValidationResult.valid();
    }
}
