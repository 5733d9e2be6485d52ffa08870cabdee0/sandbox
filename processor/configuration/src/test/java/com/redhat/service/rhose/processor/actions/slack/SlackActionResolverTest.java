package com.redhat.service.rhose.processor.actions.slack;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.rhose.infra.models.actions.BaseAction;
import com.redhat.service.rhose.processor.actions.ActionService;
import com.redhat.service.rhose.processor.actions.kafkatopic.KafkaTopicAction;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@QuarkusTest
class SlackActionResolverTest {

    private static final String TEST_BRIDGE_ID = "test-bridge-id";
    private static final String TEST_CUSTOMER_ID = "test-customer-id";
    private static final String TEST_PROCESSOR_ID = "test-processor-id";
    private static final String TEST_PROCESSOR_TOPIC_NAME = "ob-test-processor-id";
    private static final String TEST_CHANNEL_PARAM = "myChannel";
    private static final String TEST_WEBHOOK_PARAM = "myWebhook";

    @Inject
    SlackActionResolver slackActionResolver;

    @InjectMock
    ActionService actionServiceMock;

    @BeforeEach
    void beforeEach() {
        reset(actionServiceMock);

        when(actionServiceMock.getConnectorTopicName(TEST_PROCESSOR_ID)).thenReturn(TEST_PROCESSOR_TOPIC_NAME);
        when(actionServiceMock.getConnectorTopicName(not(eq(TEST_PROCESSOR_ID)))).thenThrow(new IllegalStateException());
    }

    @Test
    void testTransform() {
        BaseAction baseAction = buildTestAction();

        BaseAction transformedAction = slackActionResolver.resolve(baseAction, TEST_CUSTOMER_ID, TEST_BRIDGE_ID, TEST_PROCESSOR_ID);

        assertThat(transformedAction.getType()).isEqualTo(KafkaTopicAction.TYPE);

        Map<String, String> transformedActionParameter = transformedAction.getParameters();

        assertThat(transformedActionParameter)
                .containsEntry(SlackAction.CHANNEL_PARAM, TEST_CHANNEL_PARAM)
                .containsEntry(SlackAction.WEBHOOK_URL_PARAM, TEST_WEBHOOK_PARAM)
                .containsEntry(KafkaTopicAction.TOPIC_PARAM, TEST_PROCESSOR_TOPIC_NAME);
    }

    private BaseAction buildTestAction() {
        Map<String, String> parameters = Map.of(
                SlackAction.CHANNEL_PARAM, TEST_CHANNEL_PARAM,
                SlackAction.WEBHOOK_URL_PARAM, TEST_WEBHOOK_PARAM);

        BaseAction action = new BaseAction();
        action.setParameters(parameters);
        return action;
    }
}
