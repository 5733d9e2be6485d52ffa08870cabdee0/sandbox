package com.redhat.service.bridge.executor.actions.kafka;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.executor.actions.ActionInvoker;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

import io.cloudevents.CloudEvent;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

public class KafkaTopicActionInvoker implements ActionInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaTopicActionInvoker.class);

    private final String topic;

    private final ProcessorDTO processor;

    private final Emitter<CloudEvent> emitter;

    public KafkaTopicActionInvoker(Emitter<CloudEvent> emitter, ProcessorDTO processor, String topic) {
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
         * We use the Processor id for the key on the event to Kafka to ensure ordering of events
         * emitted by actions on this processor.
         * 
         */
        OutgoingKafkaRecordMetadata<?> metadata = OutgoingKafkaRecordMetadata.builder()
                .withTopic(topic)
                .withKey(processor.getId())
                .build();
        emitter.send(Message.of(cloudEvent).addMetadata(metadata));
        LOG.info("Emitted CloudEvent to target topic '{}' for Action on Processor '{}' on Bridge '{}'", topic, processor.getId(), processor.getBridge().getId());
    }
}
