package com.redhat.service.smartevents.processor.sources;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.sources.slack.SlackSource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.processor.sources.slack.SlackSource.CHANNEL_PARAM;
import static com.redhat.service.smartevents.processor.sources.slack.SlackSource.TOKEN_PARAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@QuarkusTest
class SourceResolverTest {

    private static final String CUSTOMER_ID = "test-customer";
    private static final String BRIDGE_ID = "br-01";
    private static final String BRIDGE_ENDPOINT = "http://www.example.com/bridge01";
    private static final String OTHER_BRIDGE_ID = "br-02";
    private static final String OTHER_BRIDGE_ENDPOINT = "not-a-valid-url";
    private static final String PROCESSOR_ID = "pr-01";

    @Inject
    SourceResolver resolver;

    @InjectMock
    GatewayConfiguratorService gatewayConfiguratorServiceMock;

    @BeforeEach
    void beforeEach() {
        reset(gatewayConfiguratorServiceMock);

        when(gatewayConfiguratorServiceMock.getBridgeEndpoint(BRIDGE_ID, CUSTOMER_ID)).thenReturn(BRIDGE_ENDPOINT);
        when(gatewayConfiguratorServiceMock.getBridgeEndpoint(OTHER_BRIDGE_ID, CUSTOMER_ID)).thenReturn(OTHER_BRIDGE_ENDPOINT);
        when(gatewayConfiguratorServiceMock.getBridgeEndpoint(not(or(eq(BRIDGE_ID), eq(OTHER_BRIDGE_ID))), eq(CUSTOMER_ID)))
                .thenThrow(new ItemNotFoundException("Bridge not found"));
        when(gatewayConfiguratorServiceMock.getBridgeEndpoint(any(), not(eq(CUSTOMER_ID)))).thenThrow(new ItemNotFoundException("Customer not found"));
    }

    @Test
    void testActionWithoutBridgeId() {
        Source inputSource = createSlackSource();
        Action resolvedAction = resolver.resolve(inputSource, CUSTOMER_ID, BRIDGE_ID, PROCESSOR_ID);

        assertThat(resolvedAction).isNotNull();
        assertThat(resolvedAction.getType()).isEqualTo(WebhookAction.TYPE);
        assertThat(resolvedAction.getParameters()).containsEntry(WebhookAction.ENDPOINT_PARAM, BRIDGE_ENDPOINT);
        assertThat(resolvedAction.getParameters()).containsEntry(WebhookAction.USE_TECHNICAL_BEARER_TOKEN_PARAM, "true");
    }

    @Test
    void testActionWithoutOtherBridgeId() {
        Source inputSource = createSlackSource();
        assertThatExceptionOfType(GatewayProviderException.class)
                .isThrownBy(() -> resolver.resolve(inputSource, CUSTOMER_ID, OTHER_BRIDGE_ID, PROCESSOR_ID));
    }

    private Source createSlackSource() {
        Source source = new Source();
        source.setType(SlackSource.TYPE);
        source.setParameters(Map.of(CHANNEL_PARAM, "channel", TOKEN_PARAM, "token"));
        return source;
    }

}
