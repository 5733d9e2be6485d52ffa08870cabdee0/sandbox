package com.redhat.service.bridge.manager.actions.connectors;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

import static org.assertj.core.api.Assertions.assertThat;

class SlackActionTransformerTest {

    private static final String TEST_BRIDGE_ID = "test-bridge-id";
    private static final String TEST_CUSTOMER_ID = "test-customer-id";
    private static final String TEST_PROCESSOR_ID = "test-processor-id";
    private static final String TEST_CHANNEL_PARAM = "myChannel";
    private static final String TEST_WEBHOOK_PARAM = "myWebhook";
    private static final String TEST_DEFAULT_TOPIC_NAME = "topicName";
    private static final String TEST_RHOAS_TOPIC_NAME = "ob-test-processor-id";

    @Test
    void testTransformWithRhoasServiceDisabled() {
        SlackActionTransformer slackActionTransformer = buildTestTransformer(false);
        BaseAction baseAction = buildTestAction();

        BaseAction transformedAction = slackActionTransformer.transform(baseAction, TEST_BRIDGE_ID, TEST_CUSTOMER_ID, TEST_PROCESSOR_ID);

        assertThat(transformedAction.getType()).isEqualTo(KafkaTopicAction.TYPE);

        Map<String, String> transformedActionParameter = transformedAction.getParameters();

        assertThat(transformedActionParameter)
                .containsEntry(SlackAction.CHANNEL_PARAMETER, TEST_CHANNEL_PARAM)
                .containsEntry(SlackAction.WEBHOOK_URL_PARAMETER, TEST_WEBHOOK_PARAM)
                .containsEntry(KafkaTopicAction.TOPIC_PARAM, TEST_DEFAULT_TOPIC_NAME);
    }

    @Test
    void testTransformWithRhoasServiceEnabled() {
        SlackActionTransformer slackActionTransformer = buildTestTransformer(true);
        BaseAction baseAction = buildTestAction();

        BaseAction transformedAction = slackActionTransformer.transform(baseAction, TEST_BRIDGE_ID, TEST_CUSTOMER_ID, TEST_PROCESSOR_ID);

        assertThat(transformedAction.getType()).isEqualTo(KafkaTopicAction.TYPE);

        Map<String, String> transformedActionParameter = transformedAction.getParameters();

        assertThat(transformedActionParameter)
                .containsEntry(SlackAction.CHANNEL_PARAMETER, TEST_CHANNEL_PARAM)
                .containsEntry(SlackAction.WEBHOOK_URL_PARAMETER, TEST_WEBHOOK_PARAM)
                .containsEntry(KafkaTopicAction.TOPIC_PARAM, TEST_RHOAS_TOPIC_NAME);
    }

    private BaseAction buildTestAction() {
        Map<String, String> parameters = Map.of(
                SlackAction.CHANNEL_PARAMETER, TEST_CHANNEL_PARAM,
                SlackAction.WEBHOOK_URL_PARAMETER, TEST_WEBHOOK_PARAM);

        BaseAction action = new BaseAction();
        action.setParameters(parameters);
        return action;
    }

    private SlackActionTransformer buildTestTransformer(boolean rhoasEnabled) {
        SlackActionTransformer transformer = new SlackActionTransformer();
        transformer.topicName = TEST_DEFAULT_TOPIC_NAME;
        transformer.rhoasEnabled = rhoasEnabled;
        return transformer;
    }
}
