package com.redhat.service.smartevents.processor.sources.aws;

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

        if (!source.hasParameter(BUCKET_NAME_OR_ARN_PARAMETER) || source.getParameter(BUCKET_NAME_OR_ARN_PARAMETER).isEmpty()) {
            return ValidationResult.invalid(INVALID_BUCKET_NAME_OR_ARN_PARAMETER_MESSAGE);
        }

        if (!source.hasParameter(REGION_PARAMETER) || source.getParameter(REGION_PARAMETER).isEmpty()) {
            return ValidationResult.invalid(INVALID_REGION_PARAMETER_MESSAGE);
        }

        if (!source.hasParameter(ACCESS_KEY_PARAMETER) || source.getParameter(ACCESS_KEY_PARAMETER).isEmpty()) {
            return ValidationResult.invalid(INVALID_ACCESS_KEY_PARAMETER_MESSAGE);
        }

        if (!source.hasParameter(SECRET_KEY_PARAMETER) || source.getParameter(SECRET_KEY_PARAMETER).isEmpty()) {
            return ValidationResult.invalid(INVALID_SECRET_KEY_PARAMETER_MESSAGE);
        }

        if (!source.hasParameter(IGNORE_BODY_PARAMETER) || source.getParameter(IGNORE_BODY_PARAMETER).isEmpty()) {
            return ValidationResult.invalid(INVALID_IGNORE_BODY_PARAMETER_MESSAGE);
        }

        if (!source.hasParameter(DELETE_AFTER_READ_PARAMETER) || source.getParameter(DELETE_AFTER_READ_PARAMETER).isEmpty()) {
            return ValidationResult.invalid(INVALID_DELETE_AFTER_READ_PARAMETER_MESSAGE);
        }

        return ValidationResult.valid();
    }
}
