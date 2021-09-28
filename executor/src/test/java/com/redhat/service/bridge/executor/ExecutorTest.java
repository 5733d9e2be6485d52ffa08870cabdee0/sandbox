package com.redhat.service.bridge.executor;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.bridge.actions.ActionInvoker;
import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.actions.ActionProviderFactory;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.models.filters.StringEquals;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExecutorTest {

    private static final FilterEvaluatorFactory filterEvaluatorFactory = new FilterEvaluatorFactoryFEEL();

    private ActionProviderFactory actionProviderFactoryMock;

    private ActionInvoker actionInvokerMock;

    @BeforeEach
    void setup() {
        actionProviderFactoryMock = mock(ActionProviderFactory.class);
        actionInvokerMock = mock(ActionInvoker.class);
        ActionProvider actionProvider = mock(ActionProvider.class);

        when(actionProvider.getActionInvoker(any(), any())).thenReturn(actionInvokerMock);

        when(actionProviderFactoryMock.getActionProvider(eq(KafkaTopicAction.TYPE))).thenReturn(actionProvider);
    }

    @Test
    public void testOnEventWithFiltersTranformationAndAction() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "value"));

        String transformationTemplate = "{\"test\": \"{data.key}\"}";

        BaseAction action = new BaseAction();
        action.setType(KafkaTopicAction.TYPE);

        ProcessorDTO processorDTO = createProcessor(filters, transformationTemplate, action);

        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, actionProviderFactoryMock);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onEvent(cloudEvent);

        verify(actionInvokerMock, times(1)).onEvent(any());
    }

    @Test
    public void testOnEventWithNoMatchingFilters() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "notTheValue"));

        BaseAction action = new BaseAction();
        action.setType(KafkaTopicAction.TYPE);

        ProcessorDTO processorDTO = createProcessor(filters, null, action);

        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, actionProviderFactoryMock);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onEvent(cloudEvent);

        verify(actionInvokerMock, times(0)).onEvent(any());
    }

    @Test
    public void testOnEventWithNullTemplate() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "value"));

        BaseAction action = new BaseAction();
        action.setType(KafkaTopicAction.TYPE);

        ProcessorDTO processorDTO = createProcessor(filters, null, action);

        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, actionProviderFactoryMock);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onEvent(cloudEvent);

        verify(actionInvokerMock, times(1)).onEvent(any());
    }

    protected CloudEvent createCloudEvent() throws JsonProcessingException {
        String jsonString = "{\"key\":\"value\"}";
        return CloudEventUtils.build("myId", "myTopic", URI.create("mySource"), "subject",
                CloudEventUtils.getMapper().readTree(jsonString));
    }

    protected ProcessorDTO createProcessor(Set<BaseFilter> filters, String transformationTemplate, BaseAction action) {
        BridgeDTO bridgeDTO = new BridgeDTO("bridgeId-1", "bridgeName-1", "test", "jrota", BridgeStatus.AVAILABLE);
        return new ProcessorDTO("processorId-1", "processorName-1", bridgeDTO, BridgeStatus.AVAILABLE, filters, transformationTemplate, action);
    }
}
