package com.redhat.service.smartevents.processor.actions.kafkatopic;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;

import io.cloudevents.CloudEvent;
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
    public void onEvent(CloudEvent originalEvent, String transformedEvent) {

        // Extract CE Headers into Kafka Headers.
        // See https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/bindings/kafka-protocol-binding.md#3231-property-names
        List<Header> ceHeaders = originalEvent.getAttributeNames()
                .stream()
                .map(n -> {
                    String key = String.format("ce_%s", n);
                    byte[] value = Objects.requireNonNull(originalEvent.getAttribute(n)).toString().getBytes(StandardCharsets.UTF_8);
                    return new RecordHeader(key, value);
                })
                .collect(Collectors.toList());
        // A transformed event is no longer a structured CloudEvent so copy across the original content-type.
        // See https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/bindings/kafka-protocol-binding.md#3-kafka-message-mapping
        if (Objects.nonNull(originalEvent.getDataContentType())) {
            ceHeaders.add(new RecordHeader("content-type", originalEvent.getDataContentType().getBytes(StandardCharsets.UTF_8)));
        }
        Headers headers = new RecordHeaders(ceHeaders);

        /*
         * As the user can specify their target topic in the Action configuration, we set
         * it in the metadata of the message we are sending.
         */
        OutgoingKafkaRecordMetadata<?> metadata = OutgoingKafkaRecordMetadata.builder()
                .withTopic(topic)
                .withHeaders(headers)
                .build();
        emitter.send(Message.of(transformedEvent).addMetadata(metadata));
        LOG.info("Emitted CloudEvent to target topic '{}' for Action on Processor '{}' on Bridge '{}'", topic, processor.getId(), processor.getBridgeId());
    }
}
