package com.redhat.service.smartevents.executor;

import java.util.stream.Stream;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.verification.VerificationMode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;

import io.cloudevents.CloudEvent;

import static com.redhat.service.smartevents.executor.ExecutorTestUtils.PLAIN_EVENT_JSON;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.createCloudEventString;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.createSinkProcessorWithResolvedAction;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.createSinkProcessorWithSameAction;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.createSourceProcessor;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecutorServiceTest {

    private static final String BROKEN_JSON = "/%=({}[][]";

    @ParameterizedTest
    @MethodSource("executorServiceTestArgs")
    @SuppressWarnings("unchecked")
    void test(ProcessorDTO processor, String inputEvent, VerificationMode wantedNumberOfOnEventInvocations) {
        Executor executorMock = mock(Executor.class);
        when(executorMock.getProcessor()).thenReturn(processor);

        ExecutorService executorService = new ExecutorService();
        executorService.executor = executorMock;
        executorService.mapper = new ObjectMapper();
        executorService.init();

        Message<String> inputMessage = mock(Message.class);
        when(inputMessage.getPayload()).thenReturn(inputEvent);

        assertThatNoException().isThrownBy(() -> executorService.processEvent(inputMessage));
        verify(executorMock, wantedNumberOfOnEventInvocations).onEvent(any(CloudEvent.class));
        verify(inputMessage).ack();
    }

    private static Stream<Arguments> executorServiceTestArgs() {
        Object[][] arguments = {
                { createSourceProcessor(), BROKEN_JSON, never() },
                { createSourceProcessor(), PLAIN_EVENT_JSON, times(1) },
                { createSinkProcessorWithSameAction(), BROKEN_JSON, never() },
                { createSinkProcessorWithSameAction(), PLAIN_EVENT_JSON, never() },
                { createSinkProcessorWithSameAction(), createCloudEventString(), times(1) },
                { createSinkProcessorWithResolvedAction(), BROKEN_JSON, never() },
                { createSinkProcessorWithResolvedAction(), PLAIN_EVENT_JSON, never() },
                { createSinkProcessorWithResolvedAction(), createCloudEventString(), times(1) }
        };
        return Stream.of(arguments).map(Arguments::of);
    }
}
