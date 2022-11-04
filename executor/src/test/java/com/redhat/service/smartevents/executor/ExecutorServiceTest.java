package com.redhat.service.smartevents.executor;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.verification.VerificationMode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.api.v1.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorService;

import io.cloudevents.CloudEvent;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;

import static com.redhat.service.smartevents.executor.ExecutorTestUtils.CLOUD_EVENT_SOURCE;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.CLOUD_EVENT_TYPE;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.PLAIN_EVENT_JSON;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.createCloudEventHeaders;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.createSinkProcessorWithResolvedAction;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.createSinkProcessorWithSameAction;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.createSourceProcessor;
import static org.assertj.core.api.Assertions.assertThat;
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
    void test(ProcessorDTO processor,
            String inputEvent,
            Map<String, String> headers,
            VerificationMode wantedNumberOfOnEventInvocations,
            URI expectedCloudEventSource,
            String expectedCloudEventType,
            boolean ack) {
        Executor executorMock = mock(Executor.class);
        when(executorMock.getProcessor()).thenReturn(processor);

        BridgeErrorService bridgeErrorServiceMock = mock(BridgeErrorService.class);
        when(bridgeErrorServiceMock.getError(any(Exception.class))).thenReturn(Optional.empty());

        ExecutorService executorService = new ExecutorService();
        executorService.executor = executorMock;
        executorService.mapper = new ObjectMapper();
        executorService.bridgeErrorService = bridgeErrorServiceMock;

        IncomingKafkaRecord<Integer, String> inputMessage = mock(IncomingKafkaRecord.class);
        when(inputMessage.getPayload()).thenReturn(inputEvent);
        RecordHeaders recordHeaders = new RecordHeaders();
        if (headers != null) {
            for (Map.Entry<String, String> h : headers.entrySet()) {
                recordHeaders.add(h.getKey(), h.getValue().getBytes(StandardCharsets.UTF_8));
            }
        }
        when(inputMessage.getHeaders()).thenReturn(recordHeaders);
        when(inputMessage.getKey()).thenReturn(555);

        ArgumentCaptor<CloudEvent> argumentCaptor = ArgumentCaptor.forClass(CloudEvent.class);

        assertThatNoException().isThrownBy(() -> executorService.processEvent(inputMessage));
        verify(executorMock, wantedNumberOfOnEventInvocations).onEvent(argumentCaptor.capture(), any());
        verify(inputMessage, times(ack ? 1 : 0)).ack();
        verify(inputMessage, times(ack ? 0 : 1)).nack(any(), any());

        if (!argumentCaptor.getAllValues().isEmpty()) {
            CloudEvent capturedEvent = argumentCaptor.getValue();
            assertThat(capturedEvent.getSource()).isEqualTo(expectedCloudEventSource);
            assertThat(capturedEvent.getType()).isEqualTo(expectedCloudEventType);
        }
    }

    private static Stream<Arguments> executorServiceTestArgs() {
        Object[][] arguments = {
                { createSourceProcessor(), BROKEN_JSON, null, never(), null, null, false },
                { createSourceProcessor(), null, null, times(1), URI.create(ExecutorService.CLOUD_EVENT_SOURCE), "slack_source_0.1", true },
                { createSourceProcessor(), "", null, times(1), URI.create(ExecutorService.CLOUD_EVENT_SOURCE), "slack_source_0.1", true },
                { createSourceProcessor(), PLAIN_EVENT_JSON, null, times(1), URI.create(ExecutorService.CLOUD_EVENT_SOURCE), "slack_source_0.1", true },
                { createSinkProcessorWithSameAction(), BROKEN_JSON, null, never(), null, null, false },
                { createSinkProcessorWithSameAction(), PLAIN_EVENT_JSON, null, never(), null, null, false },
                { createSinkProcessorWithSameAction(), PLAIN_EVENT_JSON, createCloudEventHeaders(), times(1), CLOUD_EVENT_SOURCE, CLOUD_EVENT_TYPE, true },
                { createSinkProcessorWithResolvedAction(), BROKEN_JSON, null, never(), null, null, false },
                { createSinkProcessorWithResolvedAction(), PLAIN_EVENT_JSON, null, never(), null, null, false, true },
                { createSinkProcessorWithResolvedAction(), PLAIN_EVENT_JSON, createCloudEventHeaders(), times(1), CLOUD_EVENT_SOURCE, CLOUD_EVENT_TYPE, true }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("metadataArgs")
    void test(Headers headers, Map<String, Object> expectedExtension) {
        Map<String, String> cloudEventExtension = ExecutorService.toExtensionsMap(headers);
        assertThat(cloudEventExtension).isEqualTo(expectedExtension);
    }

    private static Stream<Arguments> metadataArgs() {
        Object[][] arguments = {
                { headers(), Map.of() },
                { null, Map.of() },
                { headers("CamelAwsS3BucketName", "test-connector-1"), Map.of("camelawss3bucketname", "test-connector-1") },
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    private static Headers headers(String... args) {
        RecordHeaders headers = new RecordHeaders();
        for (int i = 0; i < args.length - 1; i += 2) {
            String key = args[i];
            String value = args[i + 1];
            headers.add(key, value.getBytes(StandardCharsets.UTF_8));
        }
        return headers;
    }
}
