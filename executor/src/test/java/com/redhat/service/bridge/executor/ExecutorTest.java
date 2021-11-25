package com.redhat.service.bridge.executor;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.bridge.actions.ActionInvoker;
import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.actions.ActionProviderException;
import com.redhat.service.bridge.actions.ActionProviderFactory;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.actions.webhook.WebhookAction;
import com.redhat.service.bridge.executor.filters.FilterEvaluatorFactory;
import com.redhat.service.bridge.executor.filters.FilterEvaluatorFactoryFEEL;
import com.redhat.service.bridge.executor.transformations.TransformationEvaluatorFactory;
import com.redhat.service.bridge.executor.transformations.TransformationEvaluatorFactoryQute;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.models.filters.StringEquals;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExecutorTest {

    private static final FilterEvaluatorFactory filterEvaluatorFactory = new FilterEvaluatorFactoryFEEL();

    private static final TransformationEvaluatorFactory transformationEvaluatorFactory = new TransformationEvaluatorFactoryQute();

    private MeterRegistry meterRegistry;

    private ActionProviderFactory actionProviderFactoryMock;

    private ActionInvoker actionInvokerMock;

    @BeforeEach
    void setup() {
        actionProviderFactoryMock = mock(ActionProviderFactory.class);
        actionInvokerMock = mock(ActionInvoker.class);
        ActionProvider actionProvider = mock(ActionProvider.class);

        when(actionProvider.getActionInvoker(any(), any())).thenReturn(actionInvokerMock);

        when(actionProviderFactoryMock.getActionProvider(KafkaTopicAction.TYPE)).thenReturn(actionProvider);
        when(actionProviderFactoryMock.getActionProvider(WebhookAction.TYPE)).thenReturn(actionProvider);

        when(actionProviderFactoryMock.getActionProvider(not(or(eq(KafkaTopicAction.TYPE), eq(WebhookAction.TYPE)))))
                .thenThrow(new ActionProviderException("Unknown action type"));

        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    public void testOnEventWithFiltersTransformationAndAction() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "value"));

        String transformationTemplate = "{\"test\": \"{data.key}\"}";

        BaseAction action = new BaseAction();
        action.setType(KafkaTopicAction.TYPE);

        ProcessorDTO processorDTO = createProcessor(new ProcessorDefinition(filters, transformationTemplate, action));

        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionProviderFactoryMock, meterRegistry);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onEvent(cloudEvent);

        verify(actionProviderFactoryMock).getActionProvider(KafkaTopicAction.TYPE);
        verify(actionInvokerMock).onEvent(any());
    }

    @Test
    public void testOnEventWithFiltersTransformationActionAndResolvedAction() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "value"));

        String transformationTemplate = "{\"test\": \"{data.key}\"}";

        BaseAction action = new BaseAction();
        action.setType("SendToBridge");

        BaseAction resolvedAction = new BaseAction();
        resolvedAction.setType(WebhookAction.TYPE);

        ProcessorDTO processorDTO = createProcessor(new ProcessorDefinition(filters, transformationTemplate, action, resolvedAction));

        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionProviderFactoryMock, meterRegistry);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onEvent(cloudEvent);

        verify(actionProviderFactoryMock).getActionProvider(WebhookAction.TYPE);
        verify(actionInvokerMock, times(1)).onEvent(any());
    }

    @Test
    public void testOnEventWithFiltersTransformationValidActionAndInvalidResolvedAction() throws JsonProcessingException {
        String unknownActionType = "UnknownAction";

        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "value"));

        String transformationTemplate = "{\"test\": \"{data.key}\"}";

        BaseAction action = new BaseAction();
        action.setType(KafkaTopicAction.TYPE);

        BaseAction resolvedAction = new BaseAction();
        resolvedAction.setType(unknownActionType);

        ProcessorDTO processorDTO = createProcessor(new ProcessorDefinition(filters, transformationTemplate, action, resolvedAction));

        assertThatExceptionOfType(ActionProviderException.class)
                .isThrownBy(() -> new Executor(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionProviderFactoryMock, meterRegistry));

        verify(actionProviderFactoryMock).getActionProvider(unknownActionType);
        verify(actionInvokerMock, never()).onEvent(any());
    }

    @Test
    public void testOnEventWithNoMatchingFilters() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "notTheValue"));

        BaseAction action = new BaseAction();
        action.setType(KafkaTopicAction.TYPE);

        ProcessorDTO processorDTO = createProcessor(new ProcessorDefinition(filters, null, action));

        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionProviderFactoryMock, meterRegistry);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onEvent(cloudEvent);

        verify(actionInvokerMock, never()).onEvent(any());
    }

    @Test
    public void testOnEventWithNullTemplate() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "value"));

        BaseAction action = new BaseAction();
        action.setType(KafkaTopicAction.TYPE);

        ProcessorDTO processorDTO = createProcessor(new ProcessorDefinition(filters, null, action));

        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionProviderFactoryMock, meterRegistry);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onEvent(cloudEvent);

        verify(actionProviderFactoryMock).getActionProvider(KafkaTopicAction.TYPE);
        verify(actionInvokerMock).onEvent(any());
    }

    @Test
    public void testMetricsAreProduced() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "value"));

        BaseAction action = new BaseAction();
        action.setType(KafkaTopicAction.TYPE);

        String transformationTemplate = "{\"test\": \"{data.key}\"}";

        ProcessorDTO processorDTO = createProcessor(new ProcessorDefinition(filters, transformationTemplate, action));

        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionProviderFactoryMock, meterRegistry);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onEvent(cloudEvent);

        assertThat(meterRegistry.getMeters().stream().anyMatch(x -> x.getId().getName().equals(MetricsConstants.PROCESSOR_PROCESSING_TIME_METRIC_NAME))).isTrue();
        assertThat(meterRegistry.getMeters().stream().anyMatch(x -> x.getId().getName().equals(MetricsConstants.FILTER_PROCESSING_TIME_METRIC_NAME))).isTrue();
        assertThat(meterRegistry.getMeters().stream().anyMatch(x -> x.getId().getName().equals(MetricsConstants.TRANSFORMATION_PROCESSING_TIME_METRIC_NAME))).isTrue();
        assertThat(meterRegistry.getMeters().stream().anyMatch(x -> x.getId().getName().equals(MetricsConstants.ACTION_PROCESSING_TIME_METRIC_NAME))).isTrue();
    }

    protected CloudEvent createCloudEvent() throws JsonProcessingException {
        String jsonString = "{\"key\":\"value\"}";
        return CloudEventUtils.build("myId", SpecVersion.V1, URI.create("mySource"), "subject",
                CloudEventUtils.getMapper().readTree(jsonString));
    }

    protected ProcessorDTO createProcessor(ProcessorDefinition definition) {
        BridgeDTO bridgeDTO = new BridgeDTO("bridgeId-1", "bridgeName-1", "test", "jrota", BridgeStatus.AVAILABLE);
        return new ProcessorDTO("processorId-1", "processorName-1", definition, bridgeDTO, BridgeStatus.AVAILABLE);
    }

}
