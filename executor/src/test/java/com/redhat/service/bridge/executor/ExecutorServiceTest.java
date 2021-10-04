package com.redhat.service.bridge.executor;

import java.net.URI;
import java.util.Collections;

import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.bridge.infra.BridgeCloudEventExtension;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
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
    public void handleEvent() {

        String bridgeId = "myBridge";
        ArgumentCaptor<CloudEvent> cap = ArgumentCaptor.forClass(CloudEvent.class);
        when(executorsProvider.getExecutors(any(String.class))).thenReturn(Collections.singleton(executor));

        CloudEvent cloudEvent = CloudEventBuilder
                .v1()
                .withId("foo")
                .withSource(URI.create("bar"))
                .withType("myType")
                .withExtension(new BridgeCloudEventExtension(bridgeId)).build();

        executorsService.processBridgeEvent(Message.of(CloudEventUtils.encode(cloudEvent)));

        verify(executor).onEvent(cap.capture());
        CloudEvent invokedWith = cap.getValue();

        assertThat(invokedWith.getExtension(BridgeCloudEventExtension.BRIDGE_ID)).isEqualTo("myBridge");
    }

    @Test
    public void handleEvent_processorNotInvokedIfEventForDifferentBridgeInstance() {
        String bridgeId = "myBridge";
        when(executorsProvider.getExecutors(eq(bridgeId))).thenReturn(null);

        CloudEvent cloudEvent = CloudEventBuilder
                .v1()
                .withId("foo")
                .withSource(URI.create("bar"))
                .withType("myType")
                .withExtension(BridgeCloudEventExtension.BRIDGE_ID, "anotherBridge").build();

        executorsService.processBridgeEvent(Message.of(CloudEventUtils.encode(cloudEvent)));

        verify(executor, never()).onEvent(any(CloudEvent.class));
    }
}
