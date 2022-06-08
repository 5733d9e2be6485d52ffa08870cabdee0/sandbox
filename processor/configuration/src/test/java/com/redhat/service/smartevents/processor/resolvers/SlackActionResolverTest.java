package com.redhat.service.smartevents.processor.resolvers;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.processor.resolvers.custom.SlackActionResolver;

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
    GatewayConfiguratorService gatewayConfiguratorServiceMock;

    @BeforeEach
    void beforeEach() {
        reset(gatewayConfiguratorServiceMock);

        when(gatewayConfiguratorServiceMock.getConnectorTopicName(TEST_PROCESSOR_ID)).thenReturn(TEST_PROCESSOR_TOPIC_NAME);
        when(gatewayConfiguratorServiceMock.getConnectorTopicName(not(eq(TEST_PROCESSOR_ID)))).thenThrow(new IllegalStateException());
    }

    @Test
    void testTransform() {
        Action action = buildTestAction();

        Action transformedAction = slackActionResolver.resolve(action, TEST_CUSTOMER_ID, TEST_BRIDGE_ID, TEST_PROCESSOR_ID);

        assertThat(transformedAction.getType()).isEqualTo(KafkaTopicAction.TYPE);

        assertThat(transformedAction.getParameter(SlackAction.CHANNEL_PARAM)).isEqualTo(TEST_CHANNEL_PARAM);
        assertThat(transformedAction.getParameter(SlackAction.WEBHOOK_URL_PARAM)).isEqualTo(TEST_WEBHOOK_PARAM);
        assertThat(transformedAction.getParameter(KafkaTopicAction.TOPIC_PARAM)).isEqualTo(TEST_PROCESSOR_TOPIC_NAME);
    }

    private Action buildTestAction() {
        Map<String, String> parameters = Map.of(
                SlackAction.CHANNEL_PARAM, TEST_CHANNEL_PARAM,
                SlackAction.WEBHOOK_URL_PARAM, TEST_WEBHOOK_PARAM);

        Action action = new Action();
        action.setType(SlackAction.TYPE);
        action.setMapParameters(parameters);
        return action;
    }
}
