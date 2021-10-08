package com.redhat.service.bridge.executor;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ExecutorServiceTest {

    @Inject
    ExecutorsService executorsService;

    @InjectMock
    ExecutorsProvider executorsProvider;

    Executor executor;

    @BeforeEach
    public void before() {
        executor = mock(Executor.class);
    }

    @Test
    public void handleEvent() throws JsonProcessingException {

        ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
        when(executorsProvider.getExecutors(any(String.class))).thenReturn(Collections.singleton(executor));

        CloudEvent cloudEvent = TestUtils.buildTestCloudEvent();

        executorsService.processBridgeEvent(Message.of(CloudEventUtils.encode(cloudEvent)));

        verify(executor).onEvent(cap.capture(), any(String.class));
        Map<String, Object> invokedWith = cap.getValue();

        assertThat(invokedWith.get("k1")).isEqualTo("v1");
    }

    @Test
    public void handleEvent_processorNotInvokedIfEventForDifferentBridgeInstance() throws JsonProcessingException {
        when(executorsProvider.getExecutors(eq(TestUtils.BRIDGE_ID))).thenReturn(null);

        CloudEvent cloudEvent = TestUtils.buildTestCloudEvent();

        executorsService.processBridgeEvent(Message.of(CloudEventUtils.encode(cloudEvent)));

        verify(executor, never()).onEvent(any(Map.class), any(String.class));
    }
}
