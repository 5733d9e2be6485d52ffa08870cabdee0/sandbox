package com.redhat.service.bridge.manager.actions.connectors;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.providers.InternalKafkaConfigurationProvider;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SlackActionTransformerTest {

    private static final String TEST_BRIDGE_ID = "test-bridge-id";
    private static final String TEST_CUSTOMER_ID = "test-customer-id";
    private static final String TEST_PROCESSOR_ID = "test-processor-id";
    private static final String TEST_CHANNEL_PARAM = "myChannel";
    private static final String TEST_WEBHOOK_PARAM = "myWebhook";

    @Inject
    SlackActionTransformer slackActionTransformer;

    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;

    @Test
    void testTransform() {
        BaseAction baseAction = buildTestAction();

        BaseAction transformedAction = slackActionTransformer.transform(baseAction, TEST_BRIDGE_ID, TEST_CUSTOMER_ID, TEST_PROCESSOR_ID);

        assertThat(transformedAction.getType()).isEqualTo(KafkaTopicAction.TYPE);

        Map<String, String> transformedActionParameter = transformedAction.getParameters();

        assertThat(transformedActionParameter)
                .containsEntry(SlackAction.CHANNEL_PARAMETER, TEST_CHANNEL_PARAM)
                .containsEntry(SlackAction.WEBHOOK_URL_PARAMETER, TEST_WEBHOOK_PARAM)
                .containsEntry(KafkaTopicAction.TOPIC_PARAM, internalKafkaConfigurationProvider.getTopicPrefix() + TEST_PROCESSOR_ID);
    }

    private BaseAction buildTestAction() {
        Map<String, String> parameters = Map.of(
                SlackAction.CHANNEL_PARAMETER, TEST_CHANNEL_PARAM,
                SlackAction.WEBHOOK_URL_PARAMETER, TEST_WEBHOOK_PARAM);

        BaseAction action = new BaseAction();
        action.setParameters(parameters);
        return action;
    }
}
