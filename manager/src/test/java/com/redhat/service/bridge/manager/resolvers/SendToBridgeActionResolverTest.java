package com.redhat.service.bridge.manager.resolvers;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.BridgesService;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.processor.actions.sendtobridge.SendToBridgeActionBean;
import com.redhat.service.bridge.processor.actions.webhook.WebhookActionBean;

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

    static Bridge bridge;
    static Bridge otherBridge;

    @Inject
    SendToBridgeActionResolver transformer;

    @InjectMock
    BridgesService bridgesServiceMock;

    @BeforeAll
    static void beforeAll() {
        bridge = new Bridge();
        bridge.setId(BRIDGE_ID);
        bridge.setName("bridge01");
        bridge.setCustomerId(TEST_CUSTOMER_ID);
        bridge.setStatus(ManagedResourceStatus.READY);
        bridge.setEndpoint(BRIDGE_ENDPOINT);

        otherBridge = new Bridge();
        otherBridge.setId(OTHER_BRIDGE_ID);
        otherBridge.setName("bridge02");
        otherBridge.setCustomerId(TEST_CUSTOMER_ID);
        otherBridge.setStatus(ManagedResourceStatus.READY);
        otherBridge.setEndpoint(OTHER_BRIDGE_ENDPOINT);
    }

    @BeforeEach
    void beforeEach() {
        reset(bridgesServiceMock);

        when(bridgesServiceMock.getReadyBridge(BRIDGE_ID, TEST_CUSTOMER_ID)).thenReturn(bridge);
        when(bridgesServiceMock.getReadyBridge(OTHER_BRIDGE_ID, TEST_CUSTOMER_ID)).thenReturn(otherBridge);
        when(bridgesServiceMock.getReadyBridge(UNAVAILABLE_BRIDGE_ID, TEST_CUSTOMER_ID)).thenThrow(new BridgeLifecycleException("Unavailable bridge"));
        when(bridgesServiceMock.getReadyBridge(not(or(eq(UNAVAILABLE_BRIDGE_ID), or(eq(BRIDGE_ID), eq(OTHER_BRIDGE_ID)))), eq(TEST_CUSTOMER_ID)))
                .thenThrow(new ItemNotFoundException("Bridge not found"));
        when(bridgesServiceMock.getReadyBridge(any(), not(eq(TEST_CUSTOMER_ID)))).thenThrow(new ItemNotFoundException("Customer not found"));
    }

    @Test
    void testActionWithoutBridgeId() {
        BaseAction inputAction = actionWithoutBridgeId();
        BaseAction transformedAction = transformer.resolve(inputAction, TEST_CUSTOMER_ID, bridge.getId(), "");
        assertValid(transformedAction, BRIDGE_WEBHOOK);
    }

    @Test
    void testActionWithoutOtherBridgeId() {
        BaseAction inputAction = actionWithoutBridgeId();
        BaseAction transformedAction = transformer.resolve(inputAction, TEST_CUSTOMER_ID, otherBridge.getId(), "");
        assertValid(transformedAction, OTHER_BRIDGE_WEBHOOK);
    }

    @Test
    void testActionWithSameBridgeId() {
        BaseAction inputAction = actionWithBridgeId(bridge.getId());
        BaseAction transformedAction = transformer.resolve(inputAction, TEST_CUSTOMER_ID, bridge.getId(), "");
        assertValid(transformedAction, BRIDGE_WEBHOOK);
    }

    @Test
    void testActionWithOtherBridgeId() {
        BaseAction inputAction = actionWithBridgeId(otherBridge.getId());
        BaseAction transformedAction = transformer.resolve(inputAction, TEST_CUSTOMER_ID, bridge.getId(), "");
        assertValid(transformedAction, OTHER_BRIDGE_WEBHOOK);
    }

    @Test
    void testActionWithUnavailableBridgeId() {
        BaseAction inputAction = actionWithBridgeId(UNAVAILABLE_BRIDGE_ID);
        ProcessorRequest inputRequest = requestWithAction(inputAction);
        assertThatExceptionOfType(BridgeLifecycleException.class).isThrownBy(() -> transformer.resolve(inputRequest.getAction(), TEST_CUSTOMER_ID, bridge.getId(), ""));
    }

    @Test
    void testActionWithUnknownBridgeId() {
        BaseAction inputAction = actionWithBridgeId(UNKNOWN_BRIDGE_ID);
        ProcessorRequest inputRequest = requestWithAction(inputAction);
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> transformer.resolve(inputRequest.getAction(), TEST_CUSTOMER_ID, bridge.getId(), ""));
    }

    private void assertValid(BaseAction transformedAction, String expectedEndpoint) {
        assertThat(transformedAction).isNotNull();
        assertThat(transformedAction.getType()).isEqualTo(WebhookActionBean.TYPE);
        assertThat(transformedAction.getParameters()).containsEntry(WebhookActionBean.ENDPOINT_PARAM, expectedEndpoint);
    }

    private BaseAction actionWithoutBridgeId() {
        BaseAction action = new BaseAction();
        action.setType(SendToBridgeActionBean.TYPE);
        return action;
    }

    private BaseAction actionWithBridgeId(String bridgeId) {
        BaseAction action = actionWithoutBridgeId();
        action.getParameters().put(SendToBridgeActionBean.BRIDGE_ID_PARAM, bridgeId);
        return action;
    }

    private ProcessorRequest requestWithAction(BaseAction action) {
        ProcessorRequest request = new ProcessorRequest();
        request.setName("testProcessor");
        request.setAction(action);
        return request;
    }

}
