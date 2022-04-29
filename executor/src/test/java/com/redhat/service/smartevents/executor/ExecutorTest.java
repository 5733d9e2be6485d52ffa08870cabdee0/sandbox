package com.redhat.service.smartevents.executor;

import java.net.URI;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.executor.filters.FilterEvaluatorFactory;
import com.redhat.service.smartevents.executor.filters.FilterEvaluatorFactoryFEEL;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.CloudEventDeserializationException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.filters.StringEquals;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.infra.transformations.TransformationEvaluatorFactory;
import com.redhat.service.smartevents.infra.transformations.TransformationEvaluatorFactoryQute;
import com.redhat.service.smartevents.infra.utils.CloudEventUtils;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;
import com.redhat.service.smartevents.processor.actions.ActionInvokerBuilder;
import com.redhat.service.smartevents.processor.actions.ActionRuntime;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.sendtobridge.SendToBridgeAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.sources.slack.SlackSource;

import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecutorTest {

    private enum ExpectedOutcome {
        VALID,
        INVALID;
    }

    private static final String PLAIN_EVENT_JSON = "{\"key\":\"value\"}";

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
                .thenThrow(new GatewayProviderException("Unknown action type"));

        meterRegistry = new SimpleMeterRegistry();
    }

    @ParameterizedTest
    @MethodSource("executorParams")
    void testProcessorWithMatchingFiltersAndWithTransformationTemplate(ProcessorDTO processorDTO, String inputEvent, ExpectedOutcome expectedOutcome) {
        processorDTO.getDefinition().setFilters(Collections.singleton(new StringEquals("data.key", "value")));
        processorDTO.getDefinition().setTransformationTemplate("{\"test\": \"{data.key}\"}");

        if (expectedOutcome == ExpectedOutcome.VALID) {
            String invokedEvent = doValidTestWithInvoke(processorDTO, inputEvent);
            assertThat(invokedEvent).isEqualTo("{\"test\": \"value\"}");
        } else {
            doInvalidTestWithoutInvoke(processorDTO, inputEvent);
        }
    }

    @ParameterizedTest
    @MethodSource("executorParams")
    void testProcessorWithMatchingFiltersAndWithoutTransformationTemplate(ProcessorDTO processorDTO, String inputEvent, ExpectedOutcome expectedOutcome) {
        processorDTO.getDefinition().setFilters(Collections.singleton(new StringEquals("data.key", "value")));

        if (expectedOutcome == ExpectedOutcome.VALID) {
            String invokedEvent = doValidTestWithInvoke(processorDTO, inputEvent);
            assertThatNoException().isThrownBy(() -> CloudEventUtils.decode(invokedEvent));
        } else {
            doInvalidTestWithoutInvoke(processorDTO, inputEvent);
        }
    }

    @ParameterizedTest
    @MethodSource("executorParams")
    void testProcessorWithNonMatchingFiltersAndWithTransformationTemplate(ProcessorDTO processorDTO, String inputEvent, ExpectedOutcome expectedOutcome) {
        processorDTO.getDefinition().setFilters(Collections.singleton(new StringEquals("data.key", "notTheValue")));
        processorDTO.getDefinition().setTransformationTemplate("{\"test\": \"{data.key}\"}");

        if (expectedOutcome == ExpectedOutcome.VALID) {
            doValidTestWithoutInvoke(processorDTO, inputEvent);
        } else {
            doInvalidTestWithoutInvoke(processorDTO, inputEvent);
        }
    }

    @ParameterizedTest
    @MethodSource("executorParams")
    void testProcessorWithNonMatchingFiltersAndWithoutTransformationTemplate(ProcessorDTO processorDTO, String inputEvent, ExpectedOutcome expectedOutcome) {
        processorDTO.getDefinition().setFilters(Collections.singleton(new StringEquals("data.key", "notTheValue")));

        if (expectedOutcome == ExpectedOutcome.VALID) {
            doValidTestWithoutInvoke(processorDTO, inputEvent);
        } else {
            doInvalidTestWithoutInvoke(processorDTO, inputEvent);
        }
    }

    @ParameterizedTest
    @MethodSource("executorParams")
    void testProcessorWithoutFiltersAndWithTransformationTemplate(ProcessorDTO processorDTO, String inputEvent, ExpectedOutcome expectedOutcome) {
        processorDTO.getDefinition().setTransformationTemplate("{\"test\": \"{data.key}\"}");

        if (expectedOutcome == ExpectedOutcome.VALID) {
            String invokedEvent = doValidTestWithInvoke(processorDTO, inputEvent);
            assertThat(invokedEvent).isEqualTo("{\"test\": \"value\"}");
        } else {
            doInvalidTestWithoutInvoke(processorDTO, inputEvent);
        }
    }

    @ParameterizedTest
    @MethodSource("executorParams")
    void testProcessorWithoutFiltersAndWithoutTransformationTemplate(ProcessorDTO processorDTO, String inputEvent, ExpectedOutcome expectedOutcome) {
        if (expectedOutcome == ExpectedOutcome.VALID) {
            String invokedEvent = doValidTestWithInvoke(processorDTO, inputEvent);
            assertThatNoException().isThrownBy(() -> CloudEventUtils.decode(invokedEvent));
        } else {
            doInvalidTestWithoutInvoke(processorDTO, inputEvent);
        }
    }

    private String doValidTestWithInvoke(ProcessorDTO processorDTO, String inputEvent) {
        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionRuntime, meterRegistry, new ObjectMapper());
        executor.onEvent(inputEvent);

        assertMetricsAreInitialized();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        verify(actionRuntime).getInvokerBuilder(processorDTO.getDefinition().getResolvedAction().getType());
        verify(actionInvokerMock).onEvent(captor.capture());

        return captor.getValue();
    }

    private void doValidTestWithoutInvoke(ProcessorDTO processorDTO, String inputEvent) {
        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionRuntime, meterRegistry, new ObjectMapper());
        executor.onEvent(inputEvent);

        assertMetricsAreInitialized();

        verify(actionRuntime).getInvokerBuilder(processorDTO.getDefinition().getResolvedAction().getType());
        verify(actionInvokerMock, never()).onEvent(any());
    }

    private void doInvalidTestWithoutInvoke(ProcessorDTO processorDTO, String inputEvent) {
        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionRuntime, meterRegistry, new ObjectMapper());
        assertThatExceptionOfType(CloudEventDeserializationException.class)
                .isThrownBy(() -> executor.onEvent(inputEvent));

        assertMetricsAreInitialized();

        verify(actionRuntime).getInvokerBuilder(processorDTO.getDefinition().getResolvedAction().getType());
        verify(actionInvokerMock, never()).onEvent(any());
    }

    private void assertMetricsAreInitialized() {
        assertThat(meterRegistry.getMeters().stream().anyMatch(x -> x.getId().getName().equals(MetricsConstants.PROCESSOR_PROCESSING_TIME_METRIC_NAME))).isTrue();
        assertThat(meterRegistry.getMeters().stream().anyMatch(x -> x.getId().getName().equals(MetricsConstants.FILTER_PROCESSING_TIME_METRIC_NAME))).isTrue();
        assertThat(meterRegistry.getMeters().stream().anyMatch(x -> x.getId().getName().equals(MetricsConstants.TRANSFORMATION_PROCESSING_TIME_METRIC_NAME))).isTrue();
        assertThat(meterRegistry.getMeters().stream().anyMatch(x -> x.getId().getName().equals(MetricsConstants.ACTION_PROCESSING_TIME_METRIC_NAME))).isTrue();
    }

    private static Stream<Arguments> executorParams() {
        Object[][] arguments = {
                { createSourceProcessor(), PLAIN_EVENT_JSON, ExpectedOutcome.VALID },
                { createSinkProcessorWithSameAction(), PLAIN_EVENT_JSON, ExpectedOutcome.INVALID },
                { createSinkProcessorWithSameAction(), createCloudEvent(), ExpectedOutcome.VALID },
                { createSinkProcessorWithResolvedAction(), PLAIN_EVENT_JSON, ExpectedOutcome.INVALID },
                { createSinkProcessorWithResolvedAction(), createCloudEvent(), ExpectedOutcome.VALID }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    private static String createCloudEvent() {
        try {
            JsonNode data = CloudEventUtils.getMapper().readTree(PLAIN_EVENT_JSON);
            CloudEvent event = CloudEventUtils.build("myId", SpecVersion.V1, URI.create("mySource"), "subject", data);
            return CloudEventUtils.encode(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ProcessorDTO createSourceProcessor() {
        Source requestedSource = new Source();
        requestedSource.setType(SlackSource.TYPE);

        Action resolvedAction = new Action();
        resolvedAction.setType(WebhookAction.TYPE);

        return createProcessor(ProcessorType.SOURCE, new ProcessorDefinition(null, null, requestedSource, resolvedAction));
    }

    private static ProcessorDTO createSinkProcessorWithSameAction() {
        Action action = new Action();
        action.setType(KafkaTopicAction.TYPE);

        return createProcessor(ProcessorType.SINK, new ProcessorDefinition(null, null, action));
    }

    private static ProcessorDTO createSinkProcessorWithResolvedAction() {
        Action requestedAction = new Action();
        requestedAction.setType(SendToBridgeAction.TYPE);

        Action resolvedAction = new Action();
        resolvedAction.setType(WebhookAction.TYPE);

        return createProcessor(ProcessorType.SINK, new ProcessorDefinition(null, null, requestedAction, resolvedAction));
    }

    private static ProcessorDTO createProcessor(ProcessorType type, ProcessorDefinition definition) {
        ProcessorDTO dto = new ProcessorDTO();
        dto.setType(type);
        dto.setId("processorId-1");
        dto.setName("processorName-1");
        dto.setDefinition(definition);
        dto.setBridgeId("bridgeId-1");
        dto.setCustomerId("jrota");
        dto.setStatus(ManagedResourceStatus.READY);
        dto.setKafkaConnection(createKafkaConnection());
        return dto;
    }

    private static KafkaConnectionDTO createKafkaConnection() {
        return new KafkaConnectionDTO(
                "fake:9092",
                "test",
                "test",
                "PLAINTEXT",
                "ob-bridgeid-1");
    }
}
