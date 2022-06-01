package com.redhat.service.smartevents.executor;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.verification.VerificationMode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;

import io.cloudevents.CloudEvent;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;

import static com.redhat.service.smartevents.executor.ExecutorTestUtils.CLOUD_EVENT_SOURCE;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.CLOUD_EVENT_TYPE;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.PLAIN_EVENT_JSON;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.createCloudEventString;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.createSinkProcessorWithResolvedAction;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.createSinkProcessorWithSameAction;
import static com.redhat.service.smartevents.executor.ExecutorTestUtils.createSourceProcessor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
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
    void test(ProcessorDTO processor, String inputEvent, VerificationMode wantedNumberOfOnEventInvocations, URI expectedCloudEventSource, String expectedCloudEventType) {
        Executor executorMock = mock(Executor.class);
        when(executorMock.getProcessor()).thenReturn(processor);

        ExecutorService executorService = new ExecutorService();
        executorService.executor = executorMock;
        executorService.mapper = new ObjectMapper();

        KafkaRecord<Integer, String> inputMessage = mock(KafkaRecord.class);
        when(inputMessage.getPayload()).thenReturn(inputEvent);

        ArgumentCaptor<CloudEvent> argumentCaptor = ArgumentCaptor.forClass(CloudEvent.class);

        assertThatNoException().isThrownBy(() -> executorService.processEvent(inputMessage));
        verify(executorMock, wantedNumberOfOnEventInvocations).onEvent(argumentCaptor.capture());
        verify(inputMessage).ack();

        if (!argumentCaptor.getAllValues().isEmpty()) {
            CloudEvent capturedEvent = argumentCaptor.getValue();
            assertThat(capturedEvent.getSource()).isEqualTo(expectedCloudEventSource);
            assertThat(capturedEvent.getType()).isEqualTo(expectedCloudEventType);
        }
    }

    private static Stream<Arguments> executorServiceTestArgs() {
        Object[][] arguments = {
                { createSourceProcessor(), BROKEN_JSON, never(), null, null },
                { createSourceProcessor(), null, times(1), URI.create(ExecutorService.CLOUD_EVENT_SOURCE), "slack_source_0.1" },
                { createSourceProcessor(), "", times(1), URI.create(ExecutorService.CLOUD_EVENT_SOURCE), "slack_source_0.1" },
                { createSourceProcessor(), PLAIN_EVENT_JSON, times(1), URI.create(ExecutorService.CLOUD_EVENT_SOURCE), "slack_source_0.1" },
                { createSinkProcessorWithSameAction(), BROKEN_JSON, never(), null, null },
                { createSinkProcessorWithSameAction(), PLAIN_EVENT_JSON, never(), null, null },
                { createSinkProcessorWithSameAction(), createCloudEventString(), times(1), CLOUD_EVENT_SOURCE, CLOUD_EVENT_TYPE },
                { createSinkProcessorWithResolvedAction(), BROKEN_JSON, never(), null, null },
                { createSinkProcessorWithResolvedAction(), PLAIN_EVENT_JSON, never(), null, null },
                { createSinkProcessorWithResolvedAction(), createCloudEventString(), times(1), CLOUD_EVENT_SOURCE, CLOUD_EVENT_TYPE }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("metadataArgs")
    void test(Headers headers, Map<String, Object> expectedExtension) {
        Map<String, Object> cloudEventExtension = ExecutorService.wrapHeadersToExtensionsMap(headers);
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
