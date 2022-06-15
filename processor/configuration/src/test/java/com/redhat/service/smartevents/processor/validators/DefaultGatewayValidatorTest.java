package com.redhat.service.smartevents.processor.validators;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.processor.resolvers.AbstractGatewayValidatorTest;
import com.redhat.service.smartevents.processor.sources.aws.AwsS3Source;
import com.redhat.service.smartevents.processor.sources.slack.SlackSource;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DefaultGatewayValidatorTest extends AbstractGatewayValidatorTest {

    @Inject
    DefaultGatewayValidator validator;

    @Test
    void testAwsS3Source() {
        Map<String, String> params = new HashMap<>();
        params.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        params.put(AwsS3Source.REGION_PARAMETER, "af-south-1");
        params.put(AwsS3Source.ACCESS_KEY_PARAMETER, "access-key");
        assertValidationIsInvalid(sourceWith(AwsS3Source.TYPE, params), "$.aws_secret_key: is missing but it is required");

        params = new HashMap<>();
        params.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        params.put(AwsS3Source.REGION_PARAMETER, "af-south-1");
        params.put(AwsS3Source.ACCESS_KEY_PARAMETER, "access-key");
        params.put(AwsS3Source.SECRET_KEY_PARAMETER, "access-key");
        assertValidationIsValid(sourceWith(AwsS3Source.TYPE, params));
    }

    @Test
    void testSlackSource() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackSource.TOKEN_PARAM, "t");
        assertValidationIsInvalid(sourceWith(SlackSource.TYPE, params), "$.slack_channel: is missing but it is required");

        params = new HashMap<>();
        params.put(SlackSource.CHANNEL_PARAM, "channel");
        params.put(SlackSource.TOKEN_PARAM, "token");
        assertValidationIsValid(sourceWith(SlackSource.TYPE, params));
    }

    @Override
    protected GatewayValidator getValidator() {
        return validator;
    }
}
