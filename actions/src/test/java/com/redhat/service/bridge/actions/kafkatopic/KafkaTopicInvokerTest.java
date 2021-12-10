package com.redhat.service.bridge.actions.kafkatopic;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.bridge.infra.api.models.dto.ProcessorDTO;

import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class KafkaTopicInvokerTest {

    private ProcessorDTO createProcessor() {
        ProcessorDTO p = new ProcessorDTO();
        p.setId("myProcessor");
        p.setBridgeId("myBridge");
        return p;
    }

    @Test
    public void onEvent() {
        ArgumentCaptor<Message<String>> captor = ArgumentCaptor.forClass(Message.class);
        Emitter<String> emitter = mock(Emitter.class);
        String event = "{\"key\": \"value\"}";
        String topic = "myTestTopic";
        ProcessorDTO processor = createProcessor();

        KafkaTopicInvoker invoker = new KafkaTopicInvoker(emitter, processor, topic);
        invoker.onEvent(event);

        verify(emitter).send(captor.capture());

        Message<String> sent = captor.getValue();
        assertThat(sent.getPayload()).isEqualTo(event);

        Metadata metadata = sent.getMetadata();
        OutgoingKafkaRecordMetadata recordMetadata = metadata.get(OutgoingKafkaRecordMetadata.class).get();
        assertThat(recordMetadata.getTopic()).isEqualTo(topic);
    }
}
