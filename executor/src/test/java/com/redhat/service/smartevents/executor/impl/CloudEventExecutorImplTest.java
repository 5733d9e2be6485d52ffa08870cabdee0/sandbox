package com.redhat.service.smartevents.executor.impl;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.smartevents.executor.MetricsConstants;
import com.redhat.service.smartevents.executor.filters.FilterEvaluatorFactory;
import com.redhat.service.smartevents.executor.filters.FilterEvaluatorFactoryFEEL;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.smartevents.infra.models.actions.Action;
import com.redhat.service.smartevents.infra.models.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.filters.BaseFilter;
import com.redhat.service.smartevents.infra.models.filters.StringEquals;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.transformations.TransformationEvaluatorFactory;
import com.redhat.service.smartevents.infra.transformations.TransformationEvaluatorFactoryQute;
import com.redhat.service.smartevents.infra.utils.CloudEventUtils;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;
import com.redhat.service.smartevents.processor.actions.ActionInvokerBuilder;
import com.redhat.service.smartevents.processor.actions.ActionRuntime;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CloudEventExecutorImplTest {

    private static final FilterEvaluatorFactory filterEvaluatorFactory = new FilterEvaluatorFactoryFEEL();

    private static final TransformationEvaluatorFactory transformationEvaluatorFactory = new TransformationEvaluatorFactoryQute();

    private MeterRegistry meterRegistry;

    private ActionRuntime actionRuntime;

    private ActionInvoker actionInvokerMock;

    @BeforeEach
    void setup() {
        actionInvokerMock = mock(ActionInvoker.class);

        ActionInvokerBuilder actionInvokerBuilder = mock(ActionInvokerBuilder.class);
        when(actionInvokerBuilder.build(any(), any())).thenReturn(actionInvokerMock);

        actionRuntime = mock(ActionRuntime.class);
        when(actionRuntime.getInvokerBuilder(KafkaTopicAction.TYPE)).thenReturn(actionInvokerBuilder);
        when(actionRuntime.getInvokerBuilder(WebhookAction.TYPE)).thenReturn(actionInvokerBuilder);
        when(actionRuntime.getInvokerBuilder(not(or(eq(KafkaTopicAction.TYPE), eq(WebhookAction.TYPE)))))
                .thenThrow(new ActionProviderException("Unknown action type"));

        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    public void testOnEventWithFiltersTransformationAndSameRequestedResolvedActions() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "value"));

        String transformationTemplate = "{\"test\": \"{data.key}\"}";

        Action action = new Action();
        action.setType(KafkaTopicAction.TYPE);

        ProcessorDTO processorDTO = createProcessor(new ProcessorDefinition(filters, transformationTemplate, action));

        ActionInvoker actionInvoker = actionRuntime.getInvokerBuilder(action.getType()).build(processorDTO, action);

        CloudEventExecutorImpl executor = new CloudEventExecutorImpl(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionInvoker, meterRegistry);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onCloudEvent(cloudEvent);

        verify(actionRuntime).getInvokerBuilder(KafkaTopicAction.TYPE);
        verify(actionInvokerMock).onEvent(any());
    }

    @Test
    public void testOnEventWithFiltersTransformationAndDifferentRequestedResolvedActions() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "value"));

        String transformationTemplate = "{\"test\": \"{data.key}\"}";

        Action requestedAction = new Action();
        requestedAction.setType("SendToBridge");

        Action resolvedAction = new Action();
        resolvedAction.setType(WebhookAction.TYPE);

        ProcessorDTO processorDTO = createProcessor(new ProcessorDefinition(filters, transformationTemplate, requestedAction, resolvedAction));

        ActionInvoker actionInvoker = actionRuntime.getInvokerBuilder(resolvedAction.getType()).build(processorDTO, resolvedAction);

        CloudEventExecutorImpl executor = new CloudEventExecutorImpl(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionInvoker, meterRegistry);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onCloudEvent(cloudEvent);

        verify(actionRuntime).getInvokerBuilder(WebhookAction.TYPE);
        verify(actionInvokerMock, times(1)).onEvent(any());
    }

    @Test
    public void testOnEventWithNoMatchingFilters() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "notTheValue"));

        Action action = new Action();
        action.setType(KafkaTopicAction.TYPE);

        ProcessorDTO processorDTO = createProcessor(new ProcessorDefinition(filters, null, action));

        ActionInvoker actionInvoker = actionRuntime.getInvokerBuilder(action.getType()).build(processorDTO, action);

        CloudEventExecutorImpl executor = new CloudEventExecutorImpl(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionInvoker, meterRegistry);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onCloudEvent(cloudEvent);

        verify(actionInvokerMock, never()).onEvent(any());
    }

    @Test
    public void testOnEventWithNullTemplate() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "value"));

        Action action = new Action();
        action.setType(KafkaTopicAction.TYPE);

        ProcessorDTO processorDTO = createProcessor(new ProcessorDefinition(filters, null, action));

        ActionInvoker actionInvoker = actionRuntime.getInvokerBuilder(action.getType()).build(processorDTO, action);

        CloudEventExecutorImpl executor = new CloudEventExecutorImpl(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionInvoker, meterRegistry);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onCloudEvent(cloudEvent);

        verify(actionRuntime).getInvokerBuilder(KafkaTopicAction.TYPE);
        verify(actionInvokerMock).onEvent(any());
    }

    @Test
    public void testMetricsAreProduced() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "value"));

        Action action = new Action();
        action.setType(KafkaTopicAction.TYPE);

        String transformationTemplate = "{\"test\": \"{data.key}\"}";

        ProcessorDTO processorDTO = createProcessor(new ProcessorDefinition(filters, transformationTemplate, action));

        ActionInvoker actionInvoker = actionRuntime.getInvokerBuilder(action.getType()).build(processorDTO, action);

        CloudEventExecutorImpl executor = new CloudEventExecutorImpl(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionInvoker, meterRegistry);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onCloudEvent(cloudEvent);

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
        KafkaConnectionDTO kafkaConnectionDTO = new KafkaConnectionDTO(
                "fake:9092",
                "test",
                "test",
                "PLAINTEXT",
                "ob-bridgeid-1");
        return new ProcessorDTO("processorId-1", "processorName-1", definition, "bridgeId-1", "jrota", ManagedResourceStatus.READY, kafkaConnectionDTO);
    }
}
