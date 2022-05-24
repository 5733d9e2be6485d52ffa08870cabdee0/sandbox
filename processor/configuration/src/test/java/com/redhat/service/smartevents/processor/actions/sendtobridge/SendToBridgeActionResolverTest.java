package com.redhat.service.smartevents.processor.actions.sendtobridge;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@QuarkusTest
class SendToBridgeActionResolverTest {

    private static final String TEST_CUSTOMER_ID = "test-customer";
    private static final String BRIDGE_ID = "br-01";
    private static final String BRIDGE_ENDPOINT = "http://www.example.com/bridge01";
    private static final String BRIDGE_WEBHOOK = BRIDGE_ENDPOINT;
    private static final String OTHER_BRIDGE_ID = "br-02";
    private static final String OTHER_BRIDGE_ENDPOINT = "http://www.example.com/bridge02";
    private static final String OTHER_BRIDGE_WEBHOOK = OTHER_BRIDGE_ENDPOINT;
    private static final String UNAVAILABLE_BRIDGE_ID = "br-unavailable";
    private static final String UNKNOWN_BRIDGE_ID = "br-unknown";

    @Inject
    SendToBridgeActionResolver resolver;

    @InjectMock
    GatewayConfiguratorService gatewayConfiguratorServiceMock;

    @BeforeEach
    void beforeEach() {
        reset(gatewayConfiguratorServiceMock);

        when(gatewayConfiguratorServiceMock.getBridgeEndpoint(BRIDGE_ID, TEST_CUSTOMER_ID)).thenReturn(BRIDGE_ENDPOINT);
        when(gatewayConfiguratorServiceMock.getBridgeEndpoint(OTHER_BRIDGE_ID, TEST_CUSTOMER_ID)).thenReturn(OTHER_BRIDGE_ENDPOINT);
        when(gatewayConfiguratorServiceMock.getBridgeEndpoint(UNAVAILABLE_BRIDGE_ID, TEST_CUSTOMER_ID)).thenThrow(new BridgeLifecycleException("Unavailable bridge"));
        when(gatewayConfiguratorServiceMock.getBridgeEndpoint(not(or(eq(UNAVAILABLE_BRIDGE_ID), or(eq(BRIDGE_ID), eq(OTHER_BRIDGE_ID)))), eq(TEST_CUSTOMER_ID)))
                .thenThrow(new ItemNotFoundException("Bridge not found"));
        when(gatewayConfiguratorServiceMock.getBridgeEndpoint(any(), not(eq(TEST_CUSTOMER_ID)))).thenThrow(new ItemNotFoundException("Customer not found"));
    }

    @Test
    void testActionWithoutBridgeId() {
        Action inputAction = actionWithoutBridgeId();
        Action resolvedAction = resolver.resolve(inputAction, TEST_CUSTOMER_ID, BRIDGE_ID, "");
        assertValid(resolvedAction, BRIDGE_WEBHOOK);
    }

    @Test
    void testActionWithoutOtherBridgeId() {
        Action inputAction = actionWithoutBridgeId();
        Action resolvedAction = resolver.resolve(inputAction, TEST_CUSTOMER_ID, OTHER_BRIDGE_ID, "");
        assertValid(resolvedAction, OTHER_BRIDGE_WEBHOOK);
    }

    @Test
    void testActionWithSameBridgeId() {
        Action inputAction = actionWithBridgeId(BRIDGE_ID);
        Action resolvedAction = resolver.resolve(inputAction, TEST_CUSTOMER_ID, BRIDGE_ID, "");
        assertValid(resolvedAction, BRIDGE_WEBHOOK);
    }

    @Test
    void testActionWithOtherBridgeId() {
        Action inputAction = actionWithBridgeId(OTHER_BRIDGE_ID);
        Action resolvedAction = resolver.resolve(inputAction, TEST_CUSTOMER_ID, BRIDGE_ID, "");
        assertValid(resolvedAction, OTHER_BRIDGE_WEBHOOK);
    }

    @Test
    void testActionWithUnavailableBridgeId() {
        Action inputAction = actionWithBridgeId(UNAVAILABLE_BRIDGE_ID);
        assertThatExceptionOfType(BridgeLifecycleException.class).isThrownBy(() -> resolver.resolve(inputAction, TEST_CUSTOMER_ID, BRIDGE_ID, ""));
    }

    @Test
    void testActionWithUnknownBridgeId() {
        Action inputAction = actionWithBridgeId(UNKNOWN_BRIDGE_ID);
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> resolver.resolve(inputAction, TEST_CUSTOMER_ID, BRIDGE_ID, ""));
    }

    private void assertValid(Action resolvedAction, String expectedEndpoint) {
        assertThat(resolvedAction).isNotNull();
        assertThat(resolvedAction.getType()).isEqualTo(WebhookAction.TYPE);
        assertThat(resolvedAction.getParameter(WebhookAction.ENDPOINT_PARAM)).isEqualTo(expectedEndpoint);
    }

    private Action actionWithoutBridgeId() {
        Action action = new Action();
        action.setType(SendToBridgeAction.TYPE);
        return action;
    }

    private Action actionWithBridgeId(String bridgeId) {
        Action action = actionWithoutBridgeId();
        action.getParameters().put(SendToBridgeAction.BRIDGE_ID_PARAM, bridgeId);
        return action;
    }

}
