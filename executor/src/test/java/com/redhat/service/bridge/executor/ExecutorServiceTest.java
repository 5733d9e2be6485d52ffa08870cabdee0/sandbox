package com.redhat.service.bridge.executor;

import java.net.URI;
import java.util.Collections;

import javax.inject.Inject;

import io.quarkus.test.junit.mockito.InjectMock;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.bridge.infra.BridgeCloudEventExtension;
import com.redhat.service.bridge.infra.dto.BridgeDTO;
import com.redhat.service.bridge.infra.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
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

    // TODO: when we switch to ExecutorConfigProviderImpl, replace this attribute accordingly
    @InjectMock
    ExecutorConfigProvider executorConfigProvider;

    Executor executor;

    @BeforeEach
    public void before() {
        executor = mock(Executor.class);
    }

    @Test
    public void handleEvent() {

        String bridgeId = "myBridge";
        ArgumentCaptor<CloudEvent> cap = ArgumentCaptor.forClass(CloudEvent.class);
        when(executorConfigProvider.getExecutors(any(String.class))).thenReturn(Collections.singleton(executor));

        CloudEvent cloudEvent = CloudEventBuilder
                .v1()
                .withId("foo")
                .withSource(URI.create("bar"))
                .withType("myType")
                .withExtension(new BridgeCloudEventExtension(bridgeId)).build();

        executorsService.processBridgeEvent(Message.of(CloudEventUtils.encode(cloudEvent)));

        verify(executor).onEvent(cap.capture());
        CloudEvent invokedWith = cap.getValue();

        assertThat(invokedWith.getExtension(BridgeCloudEventExtension.BRIDGE_ID), equalTo("myBridge"));
    }

    @Test
    public void handleEvent_processorNotInvokedIfEventForDifferentBridgeInstance() {
        String bridgeId = "myBridge";
        when(executorConfigProvider.getExecutors(eq(bridgeId))).thenReturn(null);

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
