package com.redhat.service.bridge.manager.actions.sendtobridge;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
class SendToBridgeActionTest {

    @Inject
    SendToBridgeAction sendToBridgeAction;

    @Test
    void testType() {
        assertThat(sendToBridgeAction.getType()).isEqualTo(SendToBridgeAction.TYPE);
    }

    @Test
    void testValidator() {
        assertThat(sendToBridgeAction.getParameterValidator()).isNotNull();
    }

    @Test
    void testInvokerExceptionWithBridgeId() {
        ProcessorDTO processor = createProcessorWithActionForEndpoint("test-bridge-id");
        BaseAction action = processor.getDefinition().getAction();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> sendToBridgeAction.getActionInvoker(processor, action));
    }

    @Test
    void testInvokerExceptionWithoutBridgeId() {
        ProcessorDTO processor = createProcessorWithParameterlessAction();
        BaseAction action = processor.getDefinition().getAction();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> sendToBridgeAction.getActionInvoker(processor, action));
    }

    private ProcessorDTO createProcessorWithParameterlessAction() {
        BaseAction action = new BaseAction();
        action.setType(SendToBridgeAction.TYPE);
        Map<String, String> params = new HashMap<>();
        action.setParameters(params);

        ProcessorDTO processor = new ProcessorDTO();
        processor.setId("myProcessor");
        processor.setDefinition(new ProcessorDefinition(null, null, action));

        BridgeDTO bridge = new BridgeDTO();
        bridge.setId("myBridge");
        processor.setBridge(bridge);

        return processor;
    }

    private ProcessorDTO createProcessorWithActionForEndpoint(String bridgeId) {
        ProcessorDTO processor = createProcessorWithParameterlessAction();
        processor.getDefinition().getAction().getParameters().put(SendToBridgeAction.BRIDGE_ID_PARAM, bridgeId);
        return processor;
    }

}
