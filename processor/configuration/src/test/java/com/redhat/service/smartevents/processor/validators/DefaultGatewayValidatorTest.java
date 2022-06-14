package com.redhat.service.smartevents.processor.validators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
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
        assertValidationIsInvalid(sourceWith(AwsS3Source.TYPE, params), List.of("$.aws_secret_key: is missing but it is required"));

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
        assertValidationIsInvalid(sourceWith(SlackSource.TYPE, params), List.of("$.slack_channel: is missing but it is required"));

        params = new HashMap<>();
        params.put(SlackSource.CHANNEL_PARAM, "channel");
        params.put(SlackSource.TOKEN_PARAM, "token");
        assertValidationIsValid(sourceWith(SlackSource.TYPE, params));
    }

    @Test
    void testKafkaTopicAction() {
        Map<String, String> invalidParams = new HashMap<>();
        invalidParams.put(KafkaTopicAction.TOPIC_PARAM, "example_topic");
        Action invalidKafkaTopicAction = actionWith(KafkaTopicAction.TYPE, invalidParams);
        assertValidationIsInvalid(invalidKafkaTopicAction, List.of("$.kafka_broker_url: is missing but it is required",
                "$.kafka_client_id: is missing but it is required",
                "$.kafka_client_secret: is missing but it is required"));

        Map<String, String> validParams = new HashMap<>();
        validParams.put(KafkaTopicAction.TOPIC_PARAM, "example_topic");
        validParams.put(KafkaTopicAction.BROKER_URL, "example-broker-url:443");
        validParams.put(KafkaTopicAction.CLIENT_ID, "example-client-id");
        validParams.put(KafkaTopicAction.CLIENT_SECRET, "example-client-secret");
        Action validKafkaTopicAction = actionWith(KafkaTopicAction.TYPE, validParams);
        assertValidationIsValid(validKafkaTopicAction);
    }

    @Override
    protected GatewayValidator getValidator() {
        return validator;
    }
}
