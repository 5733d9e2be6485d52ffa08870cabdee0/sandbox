package com.redhat.service.smartevents.processor.validators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.processor.actions.ansible.AnsibleTowerJobTemplateAction;
import com.redhat.service.smartevents.processor.actions.eventhubs.AzureEventHubsAction;
import com.redhat.service.smartevents.processor.actions.google.GooglePubSubAction;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.resolvers.AbstractGatewayValidatorTest;
import com.redhat.service.smartevents.processor.sources.aws.AwsS3Source;
import com.redhat.service.smartevents.processor.sources.aws.AwsSqsSource;
import com.redhat.service.smartevents.processor.sources.azure.AzureEventHubSource;
import com.redhat.service.smartevents.processor.sources.google.GooglePubSubSource;
import com.redhat.service.smartevents.processor.sources.slack.SlackSource;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DefaultGatewayValidatorTest extends AbstractGatewayValidatorTest {

    @Inject
    DefaultGatewayValidator validator;

    @Test
    void testAwsS3Source() {
        Map<String, String> invalidParams = new HashMap<>();
        invalidParams.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        invalidParams.put(AwsS3Source.REGION_PARAMETER, "af-south-1");
        invalidParams.put(AwsS3Source.ACCESS_KEY_PARAMETER, "access-key");
        assertValidationIsInvalid(sourceWith(AwsS3Source.TYPE, invalidParams), List.of("$.aws_secret_key: is missing but it is required"));

        Map<String, String> validParams = new HashMap<>();
        validParams.put(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, "test-bucket-name");
        validParams.put(AwsS3Source.REGION_PARAMETER, "af-south-1");
        validParams.put(AwsS3Source.ACCESS_KEY_PARAMETER, "access-key");
        validParams.put(AwsS3Source.SECRET_KEY_PARAMETER, "access-key");
        assertValidationIsValid(sourceWith(AwsS3Source.TYPE, validParams));
    }

    @Test
    void testSlackSource() {
        Map<String, String> invalidParams = new HashMap<>();
        invalidParams.put(SlackSource.TOKEN_PARAM, "t");
        assertValidationIsInvalid(sourceWith(SlackSource.TYPE, invalidParams), List.of("$.slack_channel: is missing but it is required"));

        Map<String, String> validParams = new HashMap<>();
        validParams.put(SlackSource.CHANNEL_PARAM, "channel");
        validParams.put(SlackSource.TOKEN_PARAM, "token");
        assertValidationIsValid(sourceWith(SlackSource.TYPE, validParams));
    }

    @Test
    void testGooglePubSubSource() {
        Map<String, String> params = new HashMap<>();
        params.put(GooglePubSubSource.GCP_SERVICE_ACCOUNT_KEY_PARAM, "key");
        params.put(GooglePubSubSource.GCP_PROJECT_ID_PARAM, "id");
        params.put(GooglePubSubSource.GCP_SUBSCRIPTION_NAME, "sub");
        assertValidationIsValid(sourceWith(GooglePubSubSource.TYPE, params));
    }

    @Test
    void testAzureEventHubSource() {
        Map<String, String> params = new HashMap<>();
        params.put(AzureEventHubSource.AZURE_NAMESPACE_NAME, "namespace");
        params.put(AzureEventHubSource.AZURE_EVENTHUB_NAME, "name");
        params.put(AzureEventHubSource.AZURE_SHARED_ACCESS_NAME, "sharedAccessName");
        params.put(AzureEventHubSource.AZURE_SHARD_ACCESS_KEY, "sharedAccessKey");
        params.put(AzureEventHubSource.AZURE_BLOB_ACCOUNT_NAME, "blobAccountName");
        params.put(AzureEventHubSource.AZURE_BLOB_ACCESS_KEY, "blobAccessKey");
        params.put(AzureEventHubSource.AZURE_BLOB_CONTAINER_NAME, "blobContainerName");
        assertValidationIsValid(sourceWith(AzureEventHubSource.TYPE, params));
    }

    @Test
    void testGooglePubsubAction() {
        Map<String, String> validParams = new HashMap<>();
        validParams.put(GooglePubSubAction.GCP_PROJECT_ID_PARAM, "id");
        validParams.put(GooglePubSubAction.GCP_SERVICE_ACCOUNT_KEY_PARAM, "key");
        validParams.put(GooglePubSubAction.GCP_DESTINATION_NAME_PARAM, "dest");
        assertValidationIsValid(actionWith(GooglePubSubAction.TYPE, validParams));
    }

    @Test
    void testSlackAction() {
        Map<String, String> invalidParams = new HashMap<>();
        invalidParams.put(SlackAction.CHANNEL_PARAM, "t");
        assertValidationIsInvalid(actionWith(SlackAction.TYPE, invalidParams), List.of("$.slack_webhook_url: is missing but it is required"));

        invalidParams.clear();
        invalidParams.put(SlackAction.CHANNEL_PARAM, "t");
        invalidParams.put(SlackAction.WEBHOOK_URL_PARAM, "notavalidurl");
        assertValidationIsInvalid(actionWith(SlackAction.TYPE, invalidParams),
                List.of("$.slack_webhook_url: does not match the regex pattern (http|https):\\/\\/([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:\\/~+#-]*[\\w@?^=%&\\/~+#-])",
                        "$.slack_webhook_url: string found, object expected"));

        Map<String, String> validParams = new HashMap<>();
        validParams.put(SlackAction.CHANNEL_PARAM, "t");
        validParams.put(SlackAction.WEBHOOK_URL_PARAM, "https://slack.webhook");
        assertValidationIsValid(actionWith(SlackAction.TYPE, validParams));
    }

    @Test
    void testAwsSqsSource() {
        Map<String, String> params = new HashMap<>();
        params.put(AwsSqsSource.AWS_REGION_PARAM, "af-south-1");
        params.put(AwsSqsSource.AWS_ACCESS_KEY_ID_PARAM, "key");
        params.put(AwsSqsSource.AWS_SECRET_ACCESS_KEY_PARAM, "secret");
        params.put(AwsSqsSource.AWS_QUEUE_URL_PARAM, "QUEUENAME");

        assertValidationIsValid(sourceWith(AwsSqsSource.TYPE, params));

        Map<String, String> validParams = new HashMap<>();
        validParams.put(AwsSqsSource.AWS_REGION_PARAM, "af-south-1");
        validParams.put(AwsSqsSource.AWS_ACCESS_KEY_ID_PARAM, "key");
        validParams.put(AwsSqsSource.AWS_SECRET_ACCESS_KEY_PARAM, "secret");
        validParams.put(AwsSqsSource.AWS_QUEUE_URL_PARAM, "https://sqs.foijsdfds-iuiu-9.amazonaws.com/432432738888/iuyiuy");

        assertValidationIsValid(sourceWith(AwsSqsSource.TYPE, validParams));
    }

    @Test
    void testWebhookAction() {
        Map<String, String> invalidParams = new HashMap<>();
        invalidParams.put(WebhookAction.ENDPOINT_PARAM, "notavalidurl");
        assertValidationIsInvalid(actionWith(WebhookAction.TYPE, invalidParams),
                List.of("$.endpoint: does not match the regex pattern (http|https):\\/\\/([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:\\/~+#-]*[\\w@?^=%&\\/~+#-])"));

        invalidParams.clear();
        invalidParams.put(WebhookAction.ENDPOINT_PARAM, "http://localhost{}>?");
        assertValidationIsInvalid(actionWith(WebhookAction.TYPE, invalidParams),
                List.of("$.endpoint: does not match the regex pattern (http|https):\\/\\/([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:\\/~+#-]*[\\w@?^=%&\\/~+#-])"));

        invalidParams.clear();
        invalidParams.put(WebhookAction.ENDPOINT_PARAM, "http://webhook.site:8080/hello");
        invalidParams.put(WebhookAction.BASIC_AUTH_PASSWORD_PARAM, "pass");
        assertValidationIsInvalid(actionWith(WebhookAction.TYPE, invalidParams),
                List.of("$: has a missing property which is dependent required {basic_auth_password=[basic_auth_username], basic_auth_username=[basic_auth_password]}"));

        Map<String, String> validParams = new HashMap<>();
        validParams.put(WebhookAction.ENDPOINT_PARAM, "http://webhook.site:8080/hello");
        validParams.put(WebhookAction.BASIC_AUTH_PASSWORD_PARAM, "pass");
        validParams.put(WebhookAction.BASIC_AUTH_USERNAME_PARAM, "user");
        assertValidationIsValid(actionWith(WebhookAction.TYPE, validParams));
    }

    @Test
    void testAnsibleAction() {
        Map<String, String> invalidParams = new HashMap<>();
        invalidParams.put(AnsibleTowerJobTemplateAction.ENDPOINT_PARAM, "notavalidurl");
        assertValidationIsInvalid(actionWith(AnsibleTowerJobTemplateAction.TYPE, invalidParams),
                List.of("$.endpoint: does not match the regex pattern (http|https):\\/\\/([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:\\/~+#-]*[\\w@?^=%&\\/~+#-])",
                        "$.job_template_id: is missing but it is required"));

        Map<String, String> validParams = new HashMap<>();
        validParams.put(AnsibleTowerJobTemplateAction.ENDPOINT_PARAM, "http://webhook.site:8080/hello");
        validParams.put(AnsibleTowerJobTemplateAction.JOB_TEMPLATE_ID_PARAM, "abcd");
        assertValidationIsValid(actionWith(AnsibleTowerJobTemplateAction.TYPE, validParams));
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

    @Test
    void testAzureEventHubAction() {
        Map<String, String> invalidParams = new HashMap<>();
        invalidParams.put(AzureEventHubsAction.AZURE_SHARED_ACCESS_KEY_PARAM, "example");
        Action invalidAzureEventHubAction = actionWith(AzureEventHubsAction.TYPE, invalidParams);
        assertValidationIsInvalid(invalidAzureEventHubAction, List.of("$.azure_namespace_name: is missing but it is required",
                "$.azure_eventhub_name: is missing but it is required",
                "$.azure_shared_access_name: is missing but it is required"));

        Map<String, String> validParams = new HashMap<>();
        validParams.put(AzureEventHubsAction.AZURE_SHARED_ACCESS_KEY_PARAM, "example");
        validParams.put(AzureEventHubsAction.AZURE_EVENTHUB_NAME_PARAM, "example");
        validParams.put(AzureEventHubsAction.AZURE_NAMESPACE_NAME_PARAM, "example");
        validParams.put(AzureEventHubsAction.AZURE_SHARED_ACCESS_NAME_PARAM, "example");
        Action validAzureEventHubAction = actionWith(AzureEventHubsAction.TYPE, validParams);
        assertValidationIsValid(validAzureEventHubAction);
    }

    @Override
    protected GatewayValidator getValidator() {
        return validator;
    }
}
