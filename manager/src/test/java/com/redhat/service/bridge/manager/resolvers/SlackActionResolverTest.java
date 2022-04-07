package com.redhat.service.bridge.manager.resolvers;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.providers.ResourceNamesProvider;
import com.redhat.service.bridge.processor.actions.kafkatopic.KafkaTopicActionBean;
import com.redhat.service.bridge.processor.actions.slack.SlackActionBean;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SlackActionBeanResolverTest {

    private static final String TEST_BRIDGE_ID = "test-bridge-id";
    private static final String TEST_CUSTOMER_ID = "test-customer-id";
    private static final String TEST_PROCESSOR_ID = "test-processor-id";
    private static final String TEST_CHANNEL_PARAM = "myChannel";
    private static final String TEST_WEBHOOK_PARAM = "myWebhook";

    @Inject
    ActionResolver slackActionResolver;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Test
    void testTransform() {
        BaseAction baseAction = buildTestAction();

        BaseAction transformedAction = slackActionResolver.resolve(baseAction, TEST_CUSTOMER_ID, TEST_BRIDGE_ID, TEST_PROCESSOR_ID);

        assertThat(transformedAction.getType()).isEqualTo(KafkaTopicActionBean.TYPE);

        Map<String, String> transformedActionParameter = transformedAction.getParameters();

        assertThat(transformedActionParameter)
                .containsEntry(SlackActionBean.CHANNEL_PARAMETER, TEST_CHANNEL_PARAM)
                .containsEntry(SlackActionBean.WEBHOOK_URL_PARAMETER, TEST_WEBHOOK_PARAM)
                .containsEntry(KafkaTopicActionBean.TOPIC_PARAM, resourceNamesProvider.getProcessorTopicName(TEST_PROCESSOR_ID));
    }

    private BaseAction buildTestAction() {
        Map<String, String> parameters = Map.of(
                SlackActionBean.CHANNEL_PARAMETER, TEST_CHANNEL_PARAM,
                SlackActionBean.WEBHOOK_URL_PARAMETER, TEST_WEBHOOK_PARAM);

        BaseAction action = new BaseAction();
        action.setParameters(parameters);
        return action;
    }
}
