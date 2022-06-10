package com.redhat.service.smartevents.processor.validators;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.resolvers.AbstractGatewayValidatorTest;
import com.redhat.service.smartevents.processor.sources.aws.AwsS3Source;
import com.redhat.service.smartevents.processor.sources.aws.AwsSqsSource;
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

    @Test
    void testSlackAction() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackAction.CHANNEL_PARAM, "t");
        assertValidationIsInvalid(actionWith(SlackAction.TYPE, params), "$.slack_webhook_url: is missing but it is required");

        params = new HashMap<>();
        params.put(SlackAction.CHANNEL_PARAM, "t");
        params.put(SlackAction.WEBHOOK_URL_PARAM, "notavalidurl");
        assertValidationIsInvalid(actionWith(SlackAction.TYPE, params),
                "$.slack_webhook_url: string found, object expected and $.slack_webhook_url: does not match the regex pattern (http|https):\\/\\/([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:\\/~+#-]*[\\w@?^=%&\\/~+#-])");

        params = new HashMap<>();
        params.put(SlackAction.CHANNEL_PARAM, "t");
        params.put(SlackAction.WEBHOOK_URL_PARAM, "https://slack.webhook");
        assertValidationIsValid(actionWith(SlackAction.TYPE, params));
    }

    @Test
    void testAwsSqsSource() {
        Map<String, String> params = new HashMap<>();
        params.put(AwsSqsSource.AWS_REGION_PARAM, "af-south-1");
        params.put(AwsSqsSource.AWS_ACCESS_KEY_ID_PARAM, "key");
        params.put(AwsSqsSource.AWS_SECRET_ACCESS_KEY_PARAM, "secret");
        params.put(AwsSqsSource.AWS_QUEUE_URL_PARAM, "notavalidurl");

        assertValidationIsInvalid(sourceWith(AwsSqsSource.TYPE, params),
                "$.aws_queue_name_or_arn: does not match the regex pattern ^(https?://[^/]*)/([0-9]{12}/)?([a-zA-Z0-9-_]+)$ and $.aws_queue_name_or_arn: does not match the regex pattern ^https://sqs\\.([a-z]+-[a-z]+-[0-9])\\.amazonaws\\.com/[0-9]{12}/([^/]+)$");

        params = new HashMap<>();
        params.put(AwsSqsSource.AWS_REGION_PARAM, "af-south-1");
        params.put(AwsSqsSource.AWS_ACCESS_KEY_ID_PARAM, "key");
        params.put(AwsSqsSource.AWS_SECRET_ACCESS_KEY_PARAM, "secret");
        params.put(AwsSqsSource.AWS_QUEUE_URL_PARAM, "https://localhost:8080");

        assertValidationIsInvalid(sourceWith(AwsSqsSource.TYPE, params),
                "$.aws_queue_name_or_arn: does not match the regex pattern ^(https?://[^/]*)/([0-9]{12}/)?([a-zA-Z0-9-_]+)$ and $.aws_queue_name_or_arn: does not match the regex pattern ^https://sqs\\.([a-z]+-[a-z]+-[0-9])\\.amazonaws\\.com/[0-9]{12}/([^/]+)$");

        params = new HashMap<>();
        params.put(AwsSqsSource.AWS_REGION_PARAM, "af-south-1");
        params.put(AwsSqsSource.AWS_ACCESS_KEY_ID_PARAM, "key");
        params.put(AwsSqsSource.AWS_SECRET_ACCESS_KEY_PARAM, "secret");
        params.put(AwsSqsSource.AWS_QUEUE_URL_PARAM, "https://sqs.foijsdfds-iuiu-9.amazonaws.com/432432738888/iuyiuy");

        assertValidationIsValid(sourceWith(AwsSqsSource.TYPE, params));
    }

    @Test
    void testWebhookAction() {
        Map<String, String> params = new HashMap<>();
        params.put(WebhookAction.ENDPOINT_PARAM, "notavalidurl");
        assertValidationIsInvalid(actionWith(WebhookAction.TYPE, params),
                "$.endpoint: does not match the regex pattern (http|https):\\/\\/([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:\\/~+#-]*[\\w@?^=%&\\/~+#-])");

        params = new HashMap<>();
        params.put(WebhookAction.ENDPOINT_PARAM, "http://localhost{}>?");
        assertValidationIsInvalid(actionWith(WebhookAction.TYPE, params),
                "$.endpoint: does not match the regex pattern (http|https):\\/\\/([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:\\/~+#-]*[\\w@?^=%&\\/~+#-])");

        params = new HashMap<>();
        params.put(WebhookAction.ENDPOINT_PARAM, "http://webhook.site:8080/hello");
        params.put(WebhookAction.BASIC_AUTH_PASSWORD_PARAM, "pass");
        assertValidationIsInvalid(actionWith(WebhookAction.TYPE, params),
                "$: has a missing property which is dependent required {basic_auth_password=[basic_auth_username], basic_auth_username=[basic_auth_password]}");

        params = new HashMap<>();
        params.put(WebhookAction.ENDPOINT_PARAM, "http://webhook.site:8080/hello");
        params.put(WebhookAction.BASIC_AUTH_PASSWORD_PARAM, "pass");
        params.put(WebhookAction.BASIC_AUTH_USERNAME_PARAM, "user");
        assertValidationIsValid(actionWith(WebhookAction.TYPE, params));
    }

    @Override
    protected GatewayValidator getValidator() {
        return validator;
    }
}
