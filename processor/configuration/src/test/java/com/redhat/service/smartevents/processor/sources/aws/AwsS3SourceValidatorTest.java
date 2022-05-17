package com.redhat.service.smartevents.processor.sources.aws;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.GatewayValidator;
import com.redhat.service.smartevents.processor.sources.AbstractSourceTest;

class AwsS3SourceValidatorTest extends AbstractSourceTest<Source> {

    private final AwsS3SourceValidator validator = new AwsS3SourceValidator();

    @Override
    protected GatewayValidator<Source> getValidator() {
        return validator;
    }

    @Override
    protected String getSourceType() {
        return AwsS3Source.TYPE;
    }

    @Test
    void isInvalidWithNoParametersOnlyFirstMessage() {
        Map<String, String> params = new HashMap<>();
        assertValidationIsInvalid(sourceWith(params), AwsS3SourceValidator.INVALID_BUCKET_NAME_OR_ARN_PARAMETER_MESSAGE);
    }

    @Test
    void isInvalidWithMissingRegionParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        assertValidationIsInvalid(sourceWith(params), AwsS3SourceValidator.INVALID_REGION_PARAMETER_MESSAGE);
    }

    @Test
    void isInvalidWithMissingAccessKey() {
        Map<String, String> params = new HashMap<>();
        params.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        params.put(AwsS3Source.REGION_PARAMETER, "test-region");
        assertValidationIsInvalid(sourceWith(params), AwsS3SourceValidator.INVALID_ACCESS_KEY_PARAMETER_MESSAGE);
    }

    @Test
    void isInvalidSecretKey() {
        Map<String, String> params = new HashMap<>();
        params.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        params.put(AwsS3Source.REGION_PARAMETER, "test-region");
        params.put(AwsS3Source.ACCESS_KEY_PARAMETER, "access-key");
        assertValidationIsInvalid(sourceWith(params), AwsS3SourceValidator.INVALID_SECRET_KEY_PARAMETER_MESSAGE);
    }

    @Test
    void isInvalidIgnoreBody() {
        Map<String, String> params = new HashMap<>();
        params.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        params.put(AwsS3Source.REGION_PARAMETER, "test-region");
        params.put(AwsS3Source.ACCESS_KEY_PARAMETER, "test-access-key");
        params.put(AwsS3Source.SECRET_KEY_PARAMETER, "test-secret-key");
        assertValidationIsInvalid(sourceWith(params), AwsS3SourceValidator.INVALID_IGNORE_BODY_PARAMETER_MESSAGE);
    }

    @Test
    void isInvalidDeleteAfterRead() {
        Map<String, String> params = new HashMap<>();
        params.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        params.put(AwsS3Source.REGION_PARAMETER, "test-region");
        params.put(AwsS3Source.ACCESS_KEY_PARAMETER, "test-access-key");
        params.put(AwsS3Source.SECRET_KEY_PARAMETER, "test-secret-key");
        params.put(AwsS3Source.IGNORE_BODY_PARAMETER, Boolean.TRUE.toString());
        assertValidationIsInvalid(sourceWith(params), AwsS3SourceValidator.INVALID_DELETE_AFTER_READ_PARAMETER_MESSAGE);
    }
}
