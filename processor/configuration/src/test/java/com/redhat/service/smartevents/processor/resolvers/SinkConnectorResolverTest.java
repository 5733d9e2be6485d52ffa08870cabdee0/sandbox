package com.redhat.service.smartevents.processor.resolvers;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@QuarkusTest
class SinkConnectorResolverTest {

    private static final String TEST_BRIDGE_ID = "test-bridge-id";
    private static final String TEST_CUSTOMER_ID = "test-customer-id";
    private static final String TEST_PROCESSOR_ID = "test-processor-id";
    private static final String TEST_PROCESSOR_TOPIC_NAME = "ob-test-processor-id";
    private static final String TEST_CHANNEL_PARAM = "myChannel";
    private static final String TEST_WEBHOOK_PARAM = "myWebhook";

    private static final String TEST_BROKER_URL = "testBrokerUrl";
    private static final String TEST_CLIENT_ID = "testClientId";
    private static final String TEST_CLIENT_SECRET = "testClientSecret";
    private static final String TEST_SECURITY_PROTOCOL = "testSecurityProtocol";

    @Inject
    SinkConnectorResolver sinkConnectorResolver;

    @InjectMock
    GatewayConfiguratorService gatewayConfiguratorServiceMock;

    @InjectMock
    GatewayConfiguratorService gatewayConfiguratorService;

    @BeforeEach
    void beforeEach() {
        reset(gatewayConfiguratorServiceMock);

        when(gatewayConfiguratorServiceMock.getConnectorTopicName(TEST_PROCESSOR_ID, "actionName")).thenReturn(TEST_PROCESSOR_TOPIC_NAME);
        when(gatewayConfiguratorServiceMock.getConnectorTopicName(not(eq(TEST_PROCESSOR_ID)), "actionName")).thenThrow(new IllegalStateException());

        when(gatewayConfiguratorService.getBootstrapServers()).thenReturn(TEST_BROKER_URL);
        when(gatewayConfiguratorService.getClientId()).thenReturn(TEST_CLIENT_ID);
        when(gatewayConfiguratorService.getClientSecret()).thenReturn(TEST_CLIENT_SECRET);
        when(gatewayConfiguratorService.getSecurityProtocol()).thenReturn(TEST_SECURITY_PROTOCOL);
    }

    @Test
    void testTransform() {
        Action action = buildTestAction();

        Action transformedAction = sinkConnectorResolver.resolve(action, TEST_CUSTOMER_ID, TEST_BRIDGE_ID, TEST_PROCESSOR_ID);

        assertThat(transformedAction.getType()).isEqualTo(KafkaTopicAction.TYPE);

        assertThat(transformedAction.getParameter(SlackAction.CHANNEL_PARAM)).isEqualTo(TEST_CHANNEL_PARAM);
        assertThat(transformedAction.getParameter(SlackAction.WEBHOOK_URL_PARAM)).isEqualTo(TEST_WEBHOOK_PARAM);
        assertThat(transformedAction.getParameter(KafkaTopicAction.TOPIC_PARAM)).isEqualTo(TEST_PROCESSOR_TOPIC_NAME);

        assertThat(transformedAction.getParameter(KafkaTopicAction.BROKER_URL)).isEqualTo(TEST_BROKER_URL);
        assertThat(transformedAction.getParameter(KafkaTopicAction.CLIENT_ID)).isEqualTo(TEST_CLIENT_ID);
        assertThat(transformedAction.getParameter(KafkaTopicAction.CLIENT_SECRET)).isEqualTo(TEST_CLIENT_SECRET);
    }

    private Action buildTestAction() {
        Map<String, String> parameters = Map.of(
                SlackAction.CHANNEL_PARAM, TEST_CHANNEL_PARAM,
                SlackAction.WEBHOOK_URL_PARAM, TEST_WEBHOOK_PARAM);

        Action action = new Action();
        action.setName("actionName");
        action.setType(SlackAction.TYPE);
        action.setMapParameters(parameters);
        return action;
    }
}
