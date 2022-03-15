package com.redhat.service.bridge.executor;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.mockito.Mockito.mock;
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

    //    @Test
    //    public void handleEvent() {
    //        CloudEvent cloudEvent = CloudEventBuilder
    //                .v1()
    //                .withId("foo")
    //                .withSource(URI.create("bar"))
    //                .withType("myType")
    //                .build();
    //
    //        executorsService.processBridgeEvent(Message.of(CloudEventUtils.encode(cloudEvent)));
    //
    //        verify(executor, times(1)).onEvent(any(CloudEvent.class));
    //    }
}
