package com.redhat.service.smartevents.executor;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.CloudEventDeserializationException;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

import static com.redhat.service.smartevents.executor.CloudEventExtension.adjustExtensionName;

@ApplicationScoped
public class ExecutorService {

    /**
     * Header key for RHOSE's Bridge Id
     */
    public static final String X_RHOSE_BRIDGE_ID = "rhose-bridge-id";

    /**
     * Header key for RHOSE's Processor Id
     */
    public static final String X_RHOSE_PROCESSOR_ID = "rhose-processor-id";

    /**
     * Header key for the original ID of an Event processed by RHOSE.
     */
    public static final String X_RHOSE_ORIGINAL_EVENT_ID = "rhose-original-event-id";

    /**
     * Header key for RHOSE's BridgeError code.
     */
    public static final String X_RHOSE_ERROR_CODE = "rhose-error-id";

    /**
     * Channel used for receiving events.
     */
    public static final String EVENTS_IN_CHANNEL = "events-in";

    public static final String CLOUD_EVENT_SOURCE = "RHOSE";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorService.class);

    @Inject
    Executor executor;

    @Inject
    ObjectMapper mapper;

    @Inject
    BridgeErrorService bridgeErrorService;

    @Incoming(EVENTS_IN_CHANNEL)
    public CompletionStage<Void> processEvent(final KafkaRecord<Integer, String> message) {
        CloudEvent cloudEvent = null;
        Headers headers = message.getHeaders() == null ? new RecordHeaders() : message.getHeaders();

        try {
            String eventPayload = message.getPayload();

            cloudEvent = executor.getProcessor().getType() == ProcessorType.SOURCE
                    ? wrapToCloudEvent(eventPayload, message.getHeaders())
                    : CloudEventUtils.decode(eventPayload);

            Map<String, String> headersMap = executor.getProcessor().getType() == ProcessorType.SOURCE
                    ? Collections.emptyMap()
                    : toHeadersMap(headers);

            executor.onEvent(cloudEvent, headersMap);
        } catch (Exception e) {
            LOG.error("Processor with id '{}' on bridge '{}' failed to handle Event.",
                    executor.getProcessor().getId(), executor.getProcessor().getBridgeId(), e);

            // create trace headers value map
            Map<String, String> traceHeaders = new TreeMap<>();
            traceHeaders.put(X_RHOSE_BRIDGE_ID, executor.getProcessor().getBridgeId());
            traceHeaders.put(X_RHOSE_PROCESSOR_ID, executor.getProcessor().getId());
            traceHeaders.put(X_RHOSE_ORIGINAL_EVENT_ID, cloudEvent != null ? cloudEvent.getId() : message.getKey().toString());
            bridgeErrorService.getError(e).ifPresent(error -> traceHeaders.put(X_RHOSE_ERROR_CODE, error.getCode()));

            // Add our Kafka Headers, first removing any pre-existing ones to avoid duplication.
            // This can be replaced with w3c trace-context parameters when we add distributed tracing.
            headers.remove(X_RHOSE_BRIDGE_ID);
            headers.remove(X_RHOSE_PROCESSOR_ID);
            headers.remove(X_RHOSE_ORIGINAL_EVENT_ID);
            headers.remove(X_RHOSE_ERROR_CODE);
            traceHeaders.forEach((key, value) -> headers.add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8))));

            return message.nack(e,
                    Metadata.of(OutgoingKafkaRecordMetadata.builder().withHeaders(new RecordHeaders(headers)).build()));
        }
        return message.ack();
    }

    private CloudEvent wrapToCloudEvent(String event, Headers headers) {
        try {
            // JsonCloudEventData.wrap requires an empty JSON
            JsonNode payload = event == null ? mapper.createObjectNode() : mapper.readTree(event);

            CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withSource(URI.create(CLOUD_EVENT_SOURCE))
                    .withType(String.format("%sSource", executor.getProcessor().getDefinition().getRequestedSource().getType()))
                    .withData(JsonCloudEventData.wrap(payload));

            toExtensionsMap(headers).forEach(cloudEventBuilder::withExtension);

            return cloudEventBuilder.build();
        } catch (JsonProcessingException e2) {
            LOG.error("JsonProcessingException when generating CloudEvent for '{}'", event, e2);
            throw new CloudEventDeserializationException("Failed to generate event map");
        }
    }

    static Map<String, String> toHeadersMap(Headers headers) {
        Map<String, String> headersMap = new TreeMap<>();
        for (Header header : headers) {
            headersMap.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
        }
        return headersMap;
    }

    static Map<String, String> toExtensionsMap(Headers headers) {
        Map<String, String> extensionMap = new TreeMap<>();
        for (Map.Entry<String, String> header : toHeadersMap(headers).entrySet()) {
            extensionMap.put(adjustExtensionName(header.getKey()), header.getValue());
        }
        return extensionMap;
    }
}
