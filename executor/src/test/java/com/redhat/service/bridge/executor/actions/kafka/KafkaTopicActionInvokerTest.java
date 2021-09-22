package com.redhat.service.bridge.executor.actions.kafka;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

import io.cloudevents.CloudEvent;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class KafkaTopicActionInvokerTest {

    private ProcessorDTO createProcessor() {
        ProcessorDTO p = new ProcessorDTO();
        p.setId("myProcessor");

        BridgeDTO b = new BridgeDTO();
        b.setId("myBridge");
        p.setBridge(b);
        return p;
    }

    @Test
    public void onEvent() {

        ArgumentCaptor<Message<CloudEvent>> captor = ArgumentCaptor.forClass(Message.class);
        Emitter<CloudEvent> emitter = mock(Emitter.class);
        CloudEvent cloudEvent = mock(CloudEvent.class);
        String topic = "myTestTopic";
        ProcessorDTO processor = createProcessor();

        KafkaTopicActionInvoker invoker = new KafkaTopicActionInvoker(emitter, processor, topic);
        invoker.onEvent(cloudEvent);

        verify(emitter).send(captor.capture());

        Message<CloudEvent> sent = captor.getValue();
        Assertions.assertEquals(cloudEvent, sent.getPayload());

        Metadata metadata = sent.getMetadata();
        OutgoingKafkaRecordMetadata recordMetadata = metadata.get(OutgoingKafkaRecordMetadata.class).get();
        Assertions.assertEquals(topic, recordMetadata.getTopic());
        Assertions.assertEquals(processor.getId(), recordMetadata.getKey());
    }
}
