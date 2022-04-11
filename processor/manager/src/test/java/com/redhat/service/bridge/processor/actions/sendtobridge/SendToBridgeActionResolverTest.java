package com.redhat.service.bridge.processor.actions.sendtobridge;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.processor.actions.ActionService;
import com.redhat.service.bridge.processor.actions.webhook.WebhookAction;

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
    SendToBridgeActionResolver transformer;

    @InjectMock
    ActionService actionServiceMock;

    @BeforeEach
    void beforeEach() {
        reset(actionServiceMock);

        when(actionServiceMock.getBridgeEndpoint(BRIDGE_ID, TEST_CUSTOMER_ID)).thenReturn(BRIDGE_ENDPOINT);
        when(actionServiceMock.getBridgeEndpoint(OTHER_BRIDGE_ID, TEST_CUSTOMER_ID)).thenReturn(OTHER_BRIDGE_ENDPOINT);
        when(actionServiceMock.getBridgeEndpoint(UNAVAILABLE_BRIDGE_ID, TEST_CUSTOMER_ID)).thenThrow(new BridgeLifecycleException("Unavailable bridge"));
        when(actionServiceMock.getBridgeEndpoint(not(or(eq(UNAVAILABLE_BRIDGE_ID), or(eq(BRIDGE_ID), eq(OTHER_BRIDGE_ID)))), eq(TEST_CUSTOMER_ID)))
                .thenThrow(new ItemNotFoundException("Bridge not found"));
        when(actionServiceMock.getBridgeEndpoint(any(), not(eq(TEST_CUSTOMER_ID)))).thenThrow(new ItemNotFoundException("Customer not found"));
    }

    @Test
    void testActionWithoutBridgeId() {
        BaseAction inputAction = actionWithoutBridgeId();
        BaseAction transformedAction = transformer.resolve(inputAction, TEST_CUSTOMER_ID, BRIDGE_ID, "");
        assertValid(transformedAction, BRIDGE_WEBHOOK);
    }

    @Test
    void testActionWithoutOtherBridgeId() {
        BaseAction inputAction = actionWithoutBridgeId();
        BaseAction transformedAction = transformer.resolve(inputAction, TEST_CUSTOMER_ID, OTHER_BRIDGE_ID, "");
        assertValid(transformedAction, OTHER_BRIDGE_WEBHOOK);
    }

    @Test
    void testActionWithSameBridgeId() {
        BaseAction inputAction = actionWithBridgeId(BRIDGE_ID);
        BaseAction transformedAction = transformer.resolve(inputAction, TEST_CUSTOMER_ID, BRIDGE_ID, "");
        assertValid(transformedAction, BRIDGE_WEBHOOK);
    }

    @Test
    void testActionWithOtherBridgeId() {
        BaseAction inputAction = actionWithBridgeId(OTHER_BRIDGE_ID);
        BaseAction transformedAction = transformer.resolve(inputAction, TEST_CUSTOMER_ID, BRIDGE_ID, "");
        assertValid(transformedAction, OTHER_BRIDGE_WEBHOOK);
    }

    @Test
    void testActionWithUnavailableBridgeId() {
        BaseAction inputAction = actionWithBridgeId(UNAVAILABLE_BRIDGE_ID);
        assertThatExceptionOfType(BridgeLifecycleException.class).isThrownBy(() -> transformer.resolve(inputAction, TEST_CUSTOMER_ID, BRIDGE_ID, ""));
    }

    @Test
    void testActionWithUnknownBridgeId() {
        BaseAction inputAction = actionWithBridgeId(UNKNOWN_BRIDGE_ID);
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> transformer.resolve(inputAction, TEST_CUSTOMER_ID, BRIDGE_ID, ""));
    }

    private void assertValid(BaseAction transformedAction, String expectedEndpoint) {
        assertThat(transformedAction).isNotNull();
        assertThat(transformedAction.getType()).isEqualTo(WebhookAction.TYPE);
        assertThat(transformedAction.getParameters()).containsEntry(WebhookAction.ENDPOINT_PARAM, expectedEndpoint);
    }

    private BaseAction actionWithoutBridgeId() {
        BaseAction action = new BaseAction();
        action.setType(SendToBridgeAction.TYPE);
        return action;
    }

    private BaseAction actionWithBridgeId(String bridgeId) {
        BaseAction action = actionWithoutBridgeId();
        action.getParameters().put(SendToBridgeAction.BRIDGE_ID_PARAM, bridgeId);
        return action;
    }

}
