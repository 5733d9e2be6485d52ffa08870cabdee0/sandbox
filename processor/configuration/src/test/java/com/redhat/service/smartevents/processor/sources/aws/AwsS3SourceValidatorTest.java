package com.redhat.service.smartevents.processor.sources.aws;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class AwsS3SourceValidatorTest {

    @Inject
    AwsS3SourceValidator validator;

    @Test
    void isInvalidWithNoParametersOnlyFirstMessage() {
        Map<String, String> params = new HashMap<>();
        assertIsInvalid(sourceWith(params), AwsS3SourceValidator.INVALID_BUCKET_NAME_OR_ARN_PARAMETER_MESSAGE);
    }

    @Test
    void isInvalidWithMissingRegionParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        assertIsInvalid(sourceWith(params), AwsS3SourceValidator.INVALID_REGION_PARAMETER_MESSAGE);
    }

    @Test
    void isInvalidWithMissingAccessKey() {
        Map<String, String> params = new HashMap<>();
        params.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        params.put(AwsS3Source.REGION_PARAMETER, "test-region");
        assertIsInvalid(sourceWith(params), AwsS3SourceValidator.INVALID_ACCESS_KEY_PARAMETER_MESSAGE);
    }

    @Test
    void isInvalidSecretKey() {
        Map<String, String> params = new HashMap<>();
        params.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        params.put(AwsS3Source.REGION_PARAMETER, "test-region");
        params.put(AwsS3Source.ACCESS_KEY_PARAMETER, "access-key");
        assertIsInvalid(sourceWith(params), AwsS3SourceValidator.INVALID_SECRET_KEY_PARAMETER_MESSAGE);
    }

    @Test
    void isInvalidIgnoreBody() {
        Map<String, String> params = new HashMap<>();
        params.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        params.put(AwsS3Source.REGION_PARAMETER, "test-region");
        params.put(AwsS3Source.ACCESS_KEY_PARAMETER, "test-access-key");
        params.put(AwsS3Source.SECRET_KEY_PARAMETER, "test-secret-key");
        assertIsInvalid(sourceWith(params), AwsS3SourceValidator.INVALID_IGNORE_BODY_PARAMETER_MESSAGE);
    }

    @Test
    void isInvalidDeleteAfterRead() {
        Map<String, String> params = new HashMap<>();
        params.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        params.put(AwsS3Source.REGION_PARAMETER, "test-region");
        params.put(AwsS3Source.ACCESS_KEY_PARAMETER, "test-access-key");
        params.put(AwsS3Source.SECRET_KEY_PARAMETER, "test-secret-key");
        params.put(AwsS3Source.IGNORE_BODY_PARAMETER, Boolean.TRUE.toString());
        assertIsInvalid(sourceWith(params), AwsS3SourceValidator.INVALID_DELETE_AFTER_READ_PARAMETER_MESSAGE);
    }

    private void assertIsInvalid(Source Source, String errorMessage) {
        ValidationResult validationResult = validator.isValid(Source);
        assertThat(validationResult.isValid()).isFalse();
        if (errorMessage == null) {
            assertThat(validationResult.getMessage()).isNull();
        } else {
            assertThat(validationResult.getMessage()).startsWith(errorMessage);
        }
    }

    private static Source sourceWith(Map<String, String> params) {
        Source source = new Source();
        source.setType(AwsS3Source.TYPE);
        source.setParameters(params);
        return source;
    }
}
