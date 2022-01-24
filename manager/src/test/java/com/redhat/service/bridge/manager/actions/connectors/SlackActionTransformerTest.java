package com.redhat.service.bridge.manager.actions.connectors;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@QuarkusTest
class SlackActionTransformerTest {

    private static final String TEST_BRIDGE_ID = "test-bridge-id";
    private static final String TEST_CUSTOMER_ID = "test-customer-id";
    private static final String TEST_PROCESSOR_ID = "test-processor-id";
    private static final String TEST_CHANNEL_PARAM = "myChannel";
    private static final String TEST_WEBHOOK_PARAM = "myWebhook";
    private static final String TEST_DEFAULT_TOPIC_NAME = "topicName";
    private static final String TEST_RHOAS_TOPIC_NAME = "ob-test-processor-id";

    @InjectMock
    RhoasService rhoasServiceMock;

    @BeforeEach
    void beforeEach() {
        reset(rhoasServiceMock);
    }

    @Test
    void testTransformWithRhoasServiceDisabled() {
        when(rhoasServiceMock.isEnabled()).thenReturn(false);
        when(rhoasServiceMock.createTopicAndGrantAccessForProcessor(any(), any())).thenThrow(new IllegalStateException());

        SlackActionTransformer slackActionTransformer = buildTestTransformer();
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
        when(rhoasServiceMock.isEnabled()).thenReturn(true);
        when(rhoasServiceMock.createTopicAndGrantAccessForProcessor(TEST_PROCESSOR_ID, RhoasTopicAccessType.PRODUCER)).thenReturn(TEST_RHOAS_TOPIC_NAME);

        SlackActionTransformer slackActionTransformer = buildTestTransformer();
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

    private SlackActionTransformer buildTestTransformer() {
        SlackActionTransformer transformer = new SlackActionTransformer();
        transformer.topicName = TEST_DEFAULT_TOPIC_NAME;
        transformer.rhoasService = rhoasServiceMock;
        return transformer;
    }
}
