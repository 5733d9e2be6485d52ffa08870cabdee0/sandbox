package com.redhat.service.bridge.executor;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.bridge.executor.filters.FilterEvaluatorFactory;
import com.redhat.service.bridge.executor.filters.FilterEvaluatorFactoryFEEL;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.KafkaConnectionDTO;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.models.filters.StringEquals;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;
import com.redhat.service.bridge.infra.transformations.TransformationEvaluatorFactory;
import com.redhat.service.bridge.infra.transformations.TransformationEvaluatorFactoryQute;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;
import com.redhat.service.bridge.processor.actions.ActionInvoker;
import com.redhat.service.bridge.processor.actions.ActionInvokerBuilder;
import com.redhat.service.bridge.processor.actions.ActionInvokerBuilderFactory;
import com.redhat.service.bridge.processor.actions.kafkatopic.KafkaTopicActionBean;
import com.redhat.service.bridge.processor.actions.webhook.WebhookActionBean;

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

public class ExecutorTest {

    private static final FilterEvaluatorFactory filterEvaluatorFactory = new FilterEvaluatorFactoryFEEL();

    private static final TransformationEvaluatorFactory transformationEvaluatorFactory = new TransformationEvaluatorFactoryQute();

    private MeterRegistry meterRegistry;

    private ActionInvokerBuilderFactory actionInvokerBuilderFactory;

    private ActionInvoker actionInvokerMock;

    @BeforeEach
    void setup() {
        actionInvokerMock = mock(ActionInvoker.class);

        ActionInvokerBuilder actionInvokerBuilder = mock(ActionInvokerBuilder.class);
        when(actionInvokerBuilder.build(any(), any())).thenReturn(actionInvokerMock);

        actionInvokerBuilderFactory = mock(ActionInvokerBuilderFactory.class);
        when(actionInvokerBuilderFactory.get(KafkaTopicActionBean.TYPE)).thenReturn(actionInvokerBuilder);
        when(actionInvokerBuilderFactory.get(WebhookActionBean.TYPE)).thenReturn(actionInvokerBuilder);
        when(actionInvokerBuilderFactory.get(not(or(eq(KafkaTopicActionBean.TYPE), eq(WebhookActionBean.TYPE)))))
                .thenThrow(new ActionProviderException("Unknown action type"));

        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    public void testOnEventWithFiltersTransformationAndSameRequestedResolvedActions() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "value"));

        String transformationTemplate = "{\"test\": \"{data.key}\"}";

        BaseAction action = new BaseAction();
        action.setType(KafkaTopicActionBean.TYPE);

        ProcessorDTO processorDTO = createProcessor(new ProcessorDefinition(filters, transformationTemplate, action));

        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionInvokerBuilderFactory, meterRegistry);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onEvent(cloudEvent);

        verify(actionInvokerBuilderFactory).get(KafkaTopicActionBean.TYPE);
        verify(actionInvokerMock).onEvent(any());
    }

    @Test
    public void testOnEventWithFiltersTransformationAndDifferentRequestedResolvedActions() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "value"));

        String transformationTemplate = "{\"test\": \"{data.key}\"}";

        BaseAction requestedAction = new BaseAction();
        requestedAction.setType("SendToBridge");

        BaseAction resolvedAction = new BaseAction();
        resolvedAction.setType(WebhookActionBean.TYPE);

        ProcessorDTO processorDTO = createProcessor(new ProcessorDefinition(filters, transformationTemplate, requestedAction, resolvedAction));

        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionInvokerBuilderFactory, meterRegistry);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onEvent(cloudEvent);

        verify(actionInvokerBuilderFactory).get(WebhookActionBean.TYPE);
        verify(actionInvokerMock, times(1)).onEvent(any());
    }

    @Test
    public void testOnEventWithNoMatchingFilters() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "notTheValue"));

        BaseAction action = new BaseAction();
        action.setType(KafkaTopicActionBean.TYPE);

        ProcessorDTO processorDTO = createProcessor(new ProcessorDefinition(filters, null, action));

        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionInvokerBuilderFactory, meterRegistry);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onEvent(cloudEvent);

        verify(actionInvokerMock, never()).onEvent(any());
    }

    @Test
    public void testOnEventWithNullTemplate() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "value"));

        BaseAction action = new BaseAction();
        action.setType(KafkaTopicActionBean.TYPE);

        ProcessorDTO processorDTO = createProcessor(new ProcessorDefinition(filters, null, action));

        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionInvokerBuilderFactory, meterRegistry);

        CloudEvent cloudEvent = createCloudEvent();

        executor.onEvent(cloudEvent);

        verify(actionInvokerBuilderFactory).get(KafkaTopicActionBean.TYPE);
        verify(actionInvokerMock).onEvent(any());
    }

    @Test
    public void testMetricsAreProduced() throws JsonProcessingException {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("data.key", "value"));

        BaseAction action = new BaseAction();
        action.setType(KafkaTopicActionBean.TYPE);

        String transformationTemplate = "{\"test\": \"{data.key}\"}";

        ProcessorDTO processorDTO = createProcessor(new ProcessorDefinition(filters, transformationTemplate, action));

        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionInvokerBuilderFactory, meterRegistry);

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
        KafkaConnectionDTO kafkaConnectionDTO = new KafkaConnectionDTO(
                "fake:9092",
                "test",
                "test",
                "PLAINTEXT",
                "ob-bridgeid-1");
        return new ProcessorDTO("processorId-1", "processorName-1", definition, "bridgeId-1", "jrota", ManagedResourceStatus.READY, kafkaConnectionDTO);
    }
}
