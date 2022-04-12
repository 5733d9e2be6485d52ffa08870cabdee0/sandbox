package com.redhat.service.bridge.processor.actions.webhook;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;
import com.redhat.service.bridge.processor.actions.ActionInvoker;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
class WebhookActionInvokerBuilderTest {

    @Inject
    WebhookActionInvokerBuilder builder;

    @Test
    void testInvokerOk() {
        ProcessorDTO processor = createProcessorWithActionForEndpoint("http://www.example.com/webhook");
        ActionInvoker actionInvoker = builder.build(processor, processor.getDefinition().getResolvedAction());
        assertThat(actionInvoker)
                .isNotNull()
                .isInstanceOf(WebhookActionInvoker.class);
    }

    @Test
    void testInvokerException() {
        ProcessorDTO processor = createProcessorWithParameterlessAction();
        assertThatExceptionOfType(ActionProviderException.class)
                .isThrownBy(() -> builder.build(processor, processor.getDefinition().getResolvedAction()));
    }

    private ProcessorDTO createProcessorWithParameterlessAction() {
        BaseAction action = new BaseAction();
        action.setType(WebhookAction.TYPE);
        Map<String, String> params = new HashMap<>();
        action.setParameters(params);

        ProcessorDTO processor = new ProcessorDTO();
        processor.setId("myProcessor");
        processor.setDefinition(new ProcessorDefinition(null, null, action));
        processor.setBridgeId("myBridge");

        return processor;
    }

    private ProcessorDTO createProcessorWithActionForEndpoint(String endpoint) {
        ProcessorDTO processor = createProcessorWithParameterlessAction();
        processor.getDefinition().getResolvedAction().getParameters().put(WebhookAction.ENDPOINT_PARAM, endpoint);
        return processor;
    }

}
