package com.redhat.service.smartevents.processor.validators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.actions.ansible.AnsibleTowerJobTemplateAction;
import com.redhat.service.smartevents.processor.actions.eventhubs.AzureEventHubsAction;
import com.redhat.service.smartevents.processor.actions.google.GooglePubSubAction;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.resolvers.AbstractGatewayValidatorTest;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DefaultActionValidatorTest extends AbstractGatewayValidatorTest {

    @Inject
    DefaultActionValidator validator;

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
    protected ActionValidator getValidator() {
        return validator;
    }
}
