package com.redhat.service.bridge.processor.actions.kafkatopic;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.processor.actions.common.ActionInvoker;

import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

public class KafkaTopicActionInvoker implements ActionInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaTopicActionInvoker.class);

    private final String topic;

    private final ProcessorDTO processor;

    private final Emitter<String> emitter;

    public KafkaTopicActionInvoker(Emitter<String> emitter, ProcessorDTO processor, String topic) {
        this.emitter = emitter;
        this.topic = topic;
        this.processor = processor;
    }

    @Override
    public void onEvent(String event) {

        /*
         * As the user can specify their target topic in the Action configuration, we set
         * it in the metadata of the message we are sending.
         *
         */
        OutgoingKafkaRecordMetadata<?> metadata = OutgoingKafkaRecordMetadata.builder()
                .withTopic(topic)
                .build();
        emitter.send(Message.of(event).addMetadata(metadata));
        LOG.info("Emitted CloudEvent to target topic '{}' for Action on Processor '{}' on Bridge '{}'", topic, processor.getId(), processor.getBridgeId());
    }
}
