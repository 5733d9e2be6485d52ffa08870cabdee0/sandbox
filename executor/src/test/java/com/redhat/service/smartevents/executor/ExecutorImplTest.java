package com.redhat.service.smartevents.executor;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import com.redhat.service.smartevents.executor.filters.FilterEvaluatorFactory;
import com.redhat.service.smartevents.executor.filters.FilterEvaluatorFactoryFEEL;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.filters.StringEquals;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.infra.transformations.TransformationEvaluatorFactory;
import com.redhat.service.smartevents.infra.transformations.TransformationEvaluatorFactoryQute;
import com.redhat.service.smartevents.infra.utils.CloudEventUtils;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;
import com.redhat.service.smartevents.processor.actions.ActionInvokerBuilder;
import com.redhat.service.smartevents.processor.actions.ActionRuntime;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

import io.cloudevents.CloudEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static com.redhat.service.smartevents.executor.ExecutorTestUtils.createCloudEvent;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.createSinkProcessorWithResolvedAction;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.createSinkProcessorWithSameAction;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.createSourceProcessor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecutorImplTest {

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
    @MethodSource("executorImplTestArgs")
    void testProcessorWithMatchingFiltersAndWithTransformationTemplate(ProcessorDTO processorDTO) {
        processorDTO.getDefinition().setFilters(Collections.singleton(new StringEquals("data.key", "value")));
        processorDTO.getDefinition().setTransformationTemplate("{\"test\": \"{data.key}\"}");
        String invokedEvent = doTestWithInvoke(processorDTO, createCloudEvent());
        if (processorDTO.getType() == ProcessorType.SOURCE) {
            // transformations don't work with source processors yet
            assertThatNoException().isThrownBy(() -> CloudEventUtils.decode(invokedEvent));
        } else {
            assertThat(invokedEvent).isEqualTo("{\"test\": \"value\"}");
        }
    }

    @ParameterizedTest
    @MethodSource("executorImplTestArgs")
    void testProcessorWithMatchingFiltersAndWithoutTransformationTemplate(ProcessorDTO processorDTO) {
        processorDTO.getDefinition().setFilters(Collections.singleton(new StringEquals("data.key", "value")));
        String invokedEvent = doTestWithInvoke(processorDTO, createCloudEvent());
        assertThatNoException().isThrownBy(() -> CloudEventUtils.decode(invokedEvent));
    }

    @ParameterizedTest
    @MethodSource("executorImplTestArgs")
    void testProcessorWithNonMatchingFiltersAndWithTransformationTemplate(ProcessorDTO processorDTO) {
        processorDTO.getDefinition().setFilters(Collections.singleton(new StringEquals("data.key", "notTheValue")));
        processorDTO.getDefinition().setTransformationTemplate("{\"test\": \"{data.key}\"}");
        doTestWithoutInvoke(processorDTO, createCloudEvent());
    }

    @ParameterizedTest
    @MethodSource("executorImplTestArgs")
    void testProcessorWithNonMatchingFiltersAndWithoutTransformationTemplate(ProcessorDTO processorDTO) {
        processorDTO.getDefinition().setFilters(Collections.singleton(new StringEquals("data.key", "notTheValue")));
        doTestWithoutInvoke(processorDTO, createCloudEvent());
    }

    @ParameterizedTest
    @MethodSource("executorImplTestArgs")
    void testProcessorWithoutFiltersAndWithTransformationTemplate(ProcessorDTO processorDTO) {
        processorDTO.getDefinition().setTransformationTemplate("{\"test\": \"{data.key}\"}");
        String invokedEvent = doTestWithInvoke(processorDTO, createCloudEvent());
        if (processorDTO.getType() == ProcessorType.SOURCE) {
            // transformations don't work with source processors yet
            assertThatNoException().isThrownBy(() -> CloudEventUtils.decode(invokedEvent));
        } else {
            assertThat(invokedEvent).isEqualTo("{\"test\": \"value\"}");
        }
    }

    @ParameterizedTest
    @MethodSource("executorImplTestArgs")
    void testProcessorWithoutFiltersAndWithoutTransformationTemplate(ProcessorDTO processorDTO) {
        String invokedEvent = doTestWithInvoke(processorDTO, createCloudEvent());
        assertThatNoException().isThrownBy(() -> CloudEventUtils.decode(invokedEvent));
    }

    private String doTestWithInvoke(ProcessorDTO processorDTO, CloudEvent inputEvent) {
        ExecutorImpl executor = new ExecutorImpl(processorDTO,
                filterEvaluatorFactory,
                transformationEvaluatorFactory,
                actionRuntime,
                meterRegistry);
        executor.onEvent(inputEvent, Collections.emptyMap());

        assertMetricsAreInitialized();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        verify(actionRuntime).getInvokerBuilder(processorDTO.getDefinition().getResolvedAction().getType());
        verify(actionInvokerMock).onEvent(captor.capture(), any());

        return captor.getValue();
    }

    private void doTestWithoutInvoke(ProcessorDTO processorDTO, CloudEvent inputEvent) {
        ExecutorImpl executor = new ExecutorImpl(processorDTO,
                filterEvaluatorFactory,
                transformationEvaluatorFactory,
                actionRuntime,
                meterRegistry);
        executor.onEvent(inputEvent, Collections.emptyMap());

        assertMetricsAreInitialized();

        verify(actionRuntime).getInvokerBuilder(processorDTO.getDefinition().getResolvedAction().getType());
        verify(actionInvokerMock, never()).onEvent(any(), any());
    }

    private void assertMetricsAreInitialized() {
        assertThat(meterRegistry.getMeters().stream().anyMatch(x -> x.getId().getName().equals(MetricsConstants.PROCESSOR_PROCESSING_TIME_METRIC_NAME))).isTrue();
        assertThat(meterRegistry.getMeters().stream().anyMatch(x -> x.getId().getName().equals(MetricsConstants.FILTER_PROCESSING_TIME_METRIC_NAME))).isTrue();
        assertThat(meterRegistry.getMeters().stream().anyMatch(x -> x.getId().getName().equals(MetricsConstants.TRANSFORMATION_PROCESSING_TIME_METRIC_NAME))).isTrue();
        assertThat(meterRegistry.getMeters().stream().anyMatch(x -> x.getId().getName().equals(MetricsConstants.ACTION_PROCESSING_TIME_METRIC_NAME))).isTrue();
    }

    private static Stream<Arguments> executorImplTestArgs() {
        Object[] arguments = {
                createSourceProcessor(),
                createSinkProcessorWithSameAction(),
                createSinkProcessorWithResolvedAction()
        };
        return Stream.of(arguments).map(Arguments::of);
    }
}
