package com.redhat.service.bridge.executor;

import java.net.URI;

import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ExecutorServiceTest {

    private static final String BRIDGE_ID = "bridgeId";

    @Inject
    ExecutorsService executorsService;

    @InjectMock
    ExecutorsProvider executorsProvider;

    Executor executor;

    @BeforeEach
    public void before() {
        executor = mock(Executor.class);

        BridgeDTO bridgeDTO = mock(BridgeDTO.class);
        when(bridgeDTO.getId()).thenReturn(BRIDGE_ID);

        ProcessorDTO processorDTO = mock(ProcessorDTO.class);
        when(processorDTO.getBridgeId()).thenReturn(BRIDGE_ID);

        when(executor.getProcessor()).thenReturn(processorDTO);
        when(executorsProvider.getExecutor()).thenReturn(executor);
    }

    @Test
    public void handleEvent() {
        ArgumentCaptor<CloudEvent> cap = ArgumentCaptor.forClass(CloudEvent.class);

        CloudEvent cloudEvent = CloudEventBuilder
                .v1()
                .withId("foo")
                .withSource(URI.create("bar"))
                .withType("myType")
                .build();

        executorsService.processBridgeEvent(Message.of(CloudEventUtils.encode(cloudEvent)));

        verify(executor, times(1)).onEvent(any(CloudEvent.class));
    }
}
