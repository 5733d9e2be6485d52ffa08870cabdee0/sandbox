package com.redhat.service.smartevents.processor.actions.kafkatopic;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;

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
    public void onEvent(String bridgeId, String processorId, String originalEventId, String transformedEvent) {

        // Add our Kafka Headers.
        // This can be replaced with w3c trace-context parameters when we add distributed tracing.
        List<Header> headers = new ArrayList<>();
        headers.add(new RecordHeader(APIConstants.X_RHOSE_BRIDGE_ID, bridgeId.getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader(APIConstants.X_RHOSE_PROCESSOR_ID, processorId.getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader(APIConstants.X_RHOSE_ORIGINAL_EVENT_ID, originalEventId.getBytes(StandardCharsets.UTF_8)));

        /*
         * As the user can specify their target topic in the Action configuration, we set
         * it in the metadata of the message we are sending.
         */
        OutgoingKafkaRecordMetadata<?> metadata = OutgoingKafkaRecordMetadata.builder()
                .withTopic(topic)
                .withHeaders(new RecordHeaders(headers))
                .build();
        emitter.send(Message.of(transformedEvent).addMetadata(metadata));
        LOG.info("Emitted CloudEvent to target topic '{}' for Action on Processor '{}' on Bridge '{}'", topic, processor.getId(), processor.getBridgeId());
    }
}
