package com.redhat.service.smartevents.processingerrors;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.processingerrors.dao.ProcessingErrorDAO;
import com.redhat.service.smartevents.processingerrors.models.ProcessingError;

import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;

import static com.redhat.service.smartevents.processingerrors.ProcessingErrorHandler.RHOSE_BRIDGE_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessingErrorHandlerTest {

    private static final String TEST_BRIDGE_ID = "123";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("processingErrorHandlerTestArgs")
    void test(Map<String, String> headers, String payload, boolean persistCalled) throws JsonProcessingException {

        ProcessingErrorDAO daoMock = mock(ProcessingErrorDAO.class);

        ProcessingErrorHandler handler = new ProcessingErrorHandler();
        handler.processingErrorDAO = daoMock;
        handler.objectMapper = MAPPER;

        IncomingKafkaRecord<Integer, String> message = toRecordMock(headers, payload);

        ArgumentCaptor<ProcessingError> argumentCaptor = ArgumentCaptor.forClass(ProcessingError.class);

        assertThatNoException().isThrownBy(() -> handler.processError(message));
        verify(daoMock, times(persistCalled ? 1 : 0)).persist(argumentCaptor.capture());
        verify(message).ack();

        if (persistCalled) {
            ProcessingError capturedValue = argumentCaptor.getValue();
            assertThat(capturedValue.getBridgeId()).isEqualTo(TEST_BRIDGE_ID);
            assertThat(capturedValue.getHeaders()).isEqualTo(headers);
            if (payload == null) {
                assertThat(capturedValue.getPayload()).isNull();
            } else {
                assertThat(capturedValue.getPayload()).isEqualTo(MAPPER.readTree(payload));
            }
        }
    }

    private static Stream<Arguments> processingErrorHandlerTestArgs() {
        Object[][] arguments = {
                { null, null, false },
                { Map.of(RHOSE_BRIDGE_ID_HEADER, ""), "{\"key\": \"value\"}", false },
                { Map.of(RHOSE_BRIDGE_ID_HEADER, "   "), "{\"key\": \"value\"}", false },
                { Map.of(RHOSE_BRIDGE_ID_HEADER, TEST_BRIDGE_ID), "{\"key\": \"", false },
                { Map.of(RHOSE_BRIDGE_ID_HEADER, TEST_BRIDGE_ID), null, true },
                { Map.of(RHOSE_BRIDGE_ID_HEADER, TEST_BRIDGE_ID), "{\"key\": \"value\"}", true }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    private static IncomingKafkaRecord<Integer, String> toRecordMock(Map<String, String> headers, String payload) {
        RecordHeaders recordHeaders = new RecordHeaders();
        if (headers != null) {
            for (Map.Entry<String, String> h : headers.entrySet()) {
                recordHeaders.add(h.getKey(), h.getValue().getBytes(StandardCharsets.UTF_8));
            }
        }

        IncomingKafkaRecord<Integer, String> recordMock = mock(IncomingKafkaRecord.class);
        when(recordMock.getKey()).thenReturn(555);
        when(recordMock.getHeaders()).thenReturn(recordHeaders);
        when(recordMock.getPayload()).thenReturn(payload);

        return recordMock;
    }
}
