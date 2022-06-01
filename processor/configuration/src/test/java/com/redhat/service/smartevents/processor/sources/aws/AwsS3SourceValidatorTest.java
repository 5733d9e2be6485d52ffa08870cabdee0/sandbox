package com.redhat.service.smartevents.processor.sources.aws;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.AbstractGatewayValidator;
import com.redhat.service.smartevents.processor.sources.AbstractSourceTest;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AwsS3SourceValidatorTest extends AbstractSourceTest<Source> {

    @Inject
    AwsS3SourceValidator validator;

    @Override
    protected AbstractGatewayValidator<Source> getValidator() {
        return validator;
    }

    @Override
    protected String getSourceType() {
        return AwsS3Source.TYPE;
    }

    @Test
    void isInvalidWithNoParametersOnlyFirstMessage() {
        Map<String, String> params = new HashMap<>();
        assertValidationIsInvalid(sourceWith(params),
                "$.aws_bucket_name_or_arn: is missing but it is required and $.aws_region: is missing but it is required and $.aws_access_key: is missing but it is required and $.aws_secret_key: is missing but it is required");
    }

    @Test
    void isInvalidWithMissingRegionParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        assertValidationIsInvalid(sourceWith(params),
                "$.aws_region: is missing but it is required and $.aws_access_key: is missing but it is required and $.aws_secret_key: is missing but it is required");
    }

    @Test
    void isInvalidWithMissingAccessKey() {
        Map<String, String> params = new HashMap<>();
        params.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        params.put(AwsS3Source.REGION_PARAMETER, "af-south-1");
        assertValidationIsInvalid(sourceWith(params), "$.aws_access_key: is missing but it is required and $.aws_secret_key: is missing but it is required");
    }

    @Test
    void isInvalidSecretKey() {
        Map<String, String> params = new HashMap<>();
        params.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        params.put(AwsS3Source.REGION_PARAMETER, "af-south-1");
        params.put(AwsS3Source.ACCESS_KEY_PARAMETER, "access-key");
        assertValidationIsInvalid(sourceWith(params), "$.aws_secret_key: is missing but it is required");
    }
}
