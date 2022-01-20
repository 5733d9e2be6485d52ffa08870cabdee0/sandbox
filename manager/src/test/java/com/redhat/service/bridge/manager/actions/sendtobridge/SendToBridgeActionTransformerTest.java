package com.redhat.service.bridge.manager.actions.sendtobridge;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.ActionProviderFactory;
import com.redhat.service.bridge.actions.webhook.WebhookAction;
import com.redhat.service.bridge.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.BridgesService;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.models.Bridge;

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
class SendToBridgeActionTransformerTest {

    private static final String TEST_CUSTOMER_ID = "test-customer";
    private static final String BRIDGE_ID = "br-01";
    private static final String BRIDGE_ENDPOINT = "http://www.example.com/bridge01";
    private static final String BRIDGE_WEBHOOK = BRIDGE_ENDPOINT + "/events";
    private static final String OTHER_BRIDGE_ID = "br-02";
    private static final String OTHER_BRIDGE_ENDPOINT = "http://www.example.com/bridge02";
    private static final String OTHER_BRIDGE_WEBHOOK = OTHER_BRIDGE_ENDPOINT + "/events";
    private static final String UNAVAILABLE_BRIDGE_ID = "br-unavailable";
    private static final String UNKNOWN_BRIDGE_ID = "br-unknown";

    static Bridge bridge;
    static Bridge otherBridge;

    @Inject
    ActionProviderFactory actionProviderFactory;

    @Inject
    SendToBridgeActionTransformer transformer;

    @InjectMock
    BridgesService bridgesServiceMock;

    @BeforeAll
    static void beforeAll() {
        bridge = new Bridge();
        bridge.setId(BRIDGE_ID);
        bridge.setName("bridge01");
        bridge.setCustomerId(TEST_CUSTOMER_ID);
        bridge.setStatus(BridgeStatus.AVAILABLE);
        bridge.setEndpoint(BRIDGE_ENDPOINT);

        otherBridge = new Bridge();
        otherBridge.setId(OTHER_BRIDGE_ID);
        otherBridge.setName("bridge02");
        otherBridge.setCustomerId(TEST_CUSTOMER_ID);
        otherBridge.setStatus(BridgeStatus.AVAILABLE);
        otherBridge.setEndpoint(OTHER_BRIDGE_ENDPOINT);
    }

    @BeforeEach
    void beforeEach() {
        reset(bridgesServiceMock);

        when(bridgesServiceMock.getAvailableBridge(BRIDGE_ID, TEST_CUSTOMER_ID)).thenReturn(bridge);
        when(bridgesServiceMock.getAvailableBridge(OTHER_BRIDGE_ID, TEST_CUSTOMER_ID)).thenReturn(otherBridge);
        when(bridgesServiceMock.getAvailableBridge(UNAVAILABLE_BRIDGE_ID, TEST_CUSTOMER_ID)).thenThrow(new BridgeLifecycleException("Unavailable bridge"));
        when(bridgesServiceMock.getAvailableBridge(not(or(eq(UNAVAILABLE_BRIDGE_ID), or(eq(BRIDGE_ID), eq(OTHER_BRIDGE_ID)))), eq(TEST_CUSTOMER_ID)))
                .thenThrow(new ItemNotFoundException("Bridge not found"));
        when(bridgesServiceMock.getAvailableBridge(any(), not(eq(TEST_CUSTOMER_ID)))).thenThrow(new ItemNotFoundException("Customer not found"));
    }

    @Test
    void testActionWithoutBridgeId() {
        BaseAction inputAction = actionWithoutBridgeId();
        BaseAction transformedAction = transformer.transform(inputAction, bridge.getId(), TEST_CUSTOMER_ID, "");
        assertValid(transformedAction, inputAction.getName(), BRIDGE_WEBHOOK);
    }

    @Test
    void testActionWithoutOtherBridgeId() {
        BaseAction inputAction = actionWithoutBridgeId();
        BaseAction transformedAction = transformer.transform(inputAction, otherBridge.getId(), TEST_CUSTOMER_ID, "");
        assertValid(transformedAction, inputAction.getName(), OTHER_BRIDGE_WEBHOOK);
    }

    @Test
    void testActionWithSameBridgeId() {
        BaseAction inputAction = actionWithBridgeId(bridge.getId());
        BaseAction transformedAction = transformer.transform(inputAction, bridge.getId(), TEST_CUSTOMER_ID, "");
        assertValid(transformedAction, inputAction.getName(), BRIDGE_WEBHOOK);
    }

    @Test
    void testActionWithOtherBridgeId() {
        BaseAction inputAction = actionWithBridgeId(otherBridge.getId());
        BaseAction transformedAction = transformer.transform(inputAction, bridge.getId(), TEST_CUSTOMER_ID, "");
        assertValid(transformedAction, inputAction.getName(), OTHER_BRIDGE_WEBHOOK);
    }

    @Test
    void testActionWithUnavailableBridgeId() {
        BaseAction inputAction = actionWithBridgeId(UNAVAILABLE_BRIDGE_ID);
        ProcessorRequest inputRequest = requestWithAction(inputAction);
        assertThatExceptionOfType(BridgeLifecycleException.class).isThrownBy(() -> transformer.transform(inputRequest.getAction(), bridge.getId(), TEST_CUSTOMER_ID, ""));
    }

    @Test
    void testActionWithUnknownBridgeId() {
        BaseAction inputAction = actionWithBridgeId(UNKNOWN_BRIDGE_ID);
        ProcessorRequest inputRequest = requestWithAction(inputAction);
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> transformer.transform(inputRequest.getAction(), bridge.getId(), TEST_CUSTOMER_ID, ""));
    }

    private void assertValid(BaseAction transformedAction, String expectedName, String expectedEndpoint) {
        assertThat(transformedAction).isNotNull();
        assertThat(transformedAction.getType()).isEqualTo(WebhookAction.TYPE);
        assertThat(transformedAction.getName()).isEqualTo(expectedName);
        assertThat(transformedAction.getParameters()).containsEntry(WebhookAction.ENDPOINT_PARAM, expectedEndpoint);
    }

    private BaseAction actionWithoutBridgeId() {
        BaseAction action = new BaseAction();
        action.setType(SendToBridgeAction.TYPE);
        action.setName("testAction");
        return action;
    }

    private BaseAction actionWithBridgeId(String bridgeId) {
        BaseAction action = actionWithoutBridgeId();
        action.getParameters().put(SendToBridgeAction.BRIDGE_ID_PARAM, bridgeId);
        return action;
    }

    private ProcessorRequest requestWithAction(BaseAction action) {
        ProcessorRequest request = new ProcessorRequest();
        request.setName("testProcessor");
        request.setAction(action);
        return request;
    }

}
