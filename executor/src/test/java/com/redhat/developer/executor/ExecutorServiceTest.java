package com.redhat.developer.executor;

import java.net.URI;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.developer.infra.BridgeCloudEventExtension;
import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.infra.dto.ProcessorDTO;
import com.redhat.developer.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ExecutorServiceTest {

    @Inject
    ExecutorsService executorsService;

    ExecutorFactory executorFactory;

    Executor executor;

    @BeforeEach
    public void before() {
        executorFactory = mock(ExecutorFactory.class);
        executor = mock(Executor.class);
        QuarkusMock.installMockForType(executorFactory, ExecutorFactory.class);
    }

    private ProcessorDTO createProcessor(String bridgeId) {
        BridgeDTO bridge = new BridgeDTO();
        bridge.setId(bridgeId);

        ProcessorDTO processor = new ProcessorDTO();
        processor.setBridge(bridge);
        processor.setId("myProcessorId");
        processor.setName("myProcessorName");
        return processor;
    }

    @Test
    public void handleEvent() {

        ArgumentCaptor<CloudEvent> cap = ArgumentCaptor.forClass(CloudEvent.class);
        when(executorFactory.createExecutor(any(ProcessorDTO.class))).thenReturn(executor);
        String bridgeId = "myBridge";

        CloudEvent cloudEvent = CloudEventBuilder
                .v1()
                .withId("foo")
                .withSource(URI.create("bar"))
                .withType("myType")
                .withExtension(new BridgeCloudEventExtension(bridgeId)).build();

        ProcessorDTO processor = createProcessor(bridgeId);

        executorsService.createExecutor(processor);
        executorsService.processBridgeEvent(CloudEventUtils.encode(cloudEvent));

        verify(executor).onEvent(cap.capture());
        CloudEvent invokedWith = cap.getValue();

        assertThat(invokedWith.getExtension(BridgeCloudEventExtension.BRIDGE_ID), equalTo("myBridge"));
    }

    @Test
    public void handleEvent_processorNotInvokedIfEventForDifferentBridgeInstance() {
        when(executorFactory.createExecutor(any(ProcessorDTO.class))).thenReturn(executor);
        String bridgeId = "myBridge";

        CloudEvent cloudEvent = CloudEventBuilder
                .v1()
                .withId("foo")
                .withSource(URI.create("bar"))
                .withType("myType")
                .withExtension(BridgeCloudEventExtension.BRIDGE_ID, "anotherBridge").build();

        ProcessorDTO processor = createProcessor(bridgeId);

        executorsService.createExecutor(processor);
        executorsService.processBridgeEvent(CloudEventUtils.encode(cloudEvent));

        verify(executor, never()).onEvent(any(CloudEvent.class));
    }
}
