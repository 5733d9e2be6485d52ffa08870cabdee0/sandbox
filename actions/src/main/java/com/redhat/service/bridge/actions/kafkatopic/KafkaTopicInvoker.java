package com.redhat.service.bridge.actions.kafkatopic;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.actions.ActionInvoker;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

import io.cloudevents.CloudEvent;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

public class KafkaTopicInvoker implements ActionInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaTopicInvoker.class);

    private final String topic;

    private final ProcessorDTO processor;

    private final Emitter<CloudEvent> emitter;

    public KafkaTopicInvoker(Emitter<CloudEvent> emitter, ProcessorDTO processor, String topic) {
        this.emitter = emitter;
        this.topic = topic;
        this.processor = processor;
    }

    @Override
    public void onEvent(CloudEvent cloudEvent) {

        /*
         * As the user can specify their target topic in the Action configuration, we set
         * it in the metadata of the message we are sending.
         *
         */
        OutgoingKafkaRecordMetadata<?> metadata = OutgoingKafkaRecordMetadata.builder()
                .withTopic(topic)
                .build();
        emitter.send(Message.of(cloudEvent).addMetadata(metadata));
        LOG.info("Emitted CloudEvent to target topic '{}' for Action on Processor '{}' on Bridge '{}'", topic, processor.getId(), processor.getBridge().getId());
    }
}
