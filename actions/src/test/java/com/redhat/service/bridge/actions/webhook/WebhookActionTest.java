package com.redhat.service.bridge.actions.webhook;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.ActionInvoker;
import com.redhat.service.bridge.actions.ActionProviderException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
public class WebhookActionTest {

    @Inject
    WebhookAction webhookAction;

    @Test
    void testInvokerOk() {
        ProcessorDTO processor = createProcessorWithActionForEndpoint("http://www.example.com/webhook");
        ActionInvoker actionInvoker = webhookAction.getActionInvoker(processor, processor.getDefinition().getAction());
        assertThat(actionInvoker)
                .isNotNull()
                .isInstanceOf(WebhookInvoker.class);
    }

    @Test
    void testInvokerException() {
        ProcessorDTO processor = createProcessorWithParameterlessAction();
        assertThatExceptionOfType(ActionProviderException.class)
                .isThrownBy(() -> webhookAction.getActionInvoker(processor, processor.getDefinition().getAction()));
    }

    private ProcessorDTO createProcessorWithParameterlessAction() {
        BaseAction action = new BaseAction();
        action.setType(WebhookAction.TYPE);
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

    private ProcessorDTO createProcessorWithActionForEndpoint(String endpoint) {
        ProcessorDTO processor = createProcessorWithParameterlessAction();
        processor.getDefinition().getAction().getParameters().put(WebhookAction.ENDPOINT_PARAM, endpoint);
        return processor;
    }

}
