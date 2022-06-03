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

import org.apache.commons.lang3.tuple.Pair;
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
import com.redhat.service.smartevents.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.BytesCloudEventData;
import io.cloudevents.jackson.JsonCloudEventData;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

import static com.redhat.service.smartevents.executor.CloudEventExtension.adjustExtensionName;
import static com.redhat.service.smartevents.infra.api.APIConstants.RHOSE_BRIDGE_ID_HEADER;
import static com.redhat.service.smartevents.infra.api.APIConstants.RHOSE_ERROR_CODE_HEADER;
import static com.redhat.service.smartevents.infra.api.APIConstants.RHOSE_ORIGINAL_EVENT_ID_HEADER;
import static com.redhat.service.smartevents.infra.api.APIConstants.RHOSE_PROCESSOR_ID_HEADER;

@ApplicationScoped
public class ExecutorService {

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
        try {
            Pair<CloudEvent, Map<String, String>> pair = convertToCloudEventAndHeadersMap(message);
            cloudEvent = pair.getLeft();
            executor.onEvent(pair.getLeft(), pair.getRight());
        } catch (Exception e) {
            LOG.error("Processor with id '{}' on bridge '{}' failed to handle Event.",
                    executor.getProcessor().getId(), executor.getProcessor().getBridgeId(), e);

            // create trace headers value map
            Map<String, String> traceHeaders = new TreeMap<>();
            traceHeaders.put(RHOSE_BRIDGE_ID_HEADER, executor.getProcessor().getBridgeId());
            traceHeaders.put(RHOSE_PROCESSOR_ID_HEADER, executor.getProcessor().getId());
            traceHeaders.put(RHOSE_ORIGINAL_EVENT_ID_HEADER, getOriginalEventId(cloudEvent, message));
            bridgeErrorService.getError(e).ifPresent(error -> traceHeaders.put(RHOSE_ERROR_CODE_HEADER, error.getCode()));

            // Add our Kafka Headers, first removing any pre-existing ones to avoid duplication.
            // This can be replaced with w3c trace-context parameters when we add distributed tracing.
            Headers headers = message.getHeaders() == null ? new RecordHeaders() : message.getHeaders();
            headers.remove(RHOSE_BRIDGE_ID_HEADER);
            headers.remove(RHOSE_PROCESSOR_ID_HEADER);
            headers.remove(RHOSE_ORIGINAL_EVENT_ID_HEADER);
            headers.remove(RHOSE_ERROR_CODE_HEADER);
            traceHeaders.forEach((key, value) -> headers.add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8))));

            return message.nack(e,
                    Metadata.of(OutgoingKafkaRecordMetadata.builder().withHeaders(new RecordHeaders(headers)).build()));
        }
        return message.ack();
    }

    private Pair<CloudEvent, Map<String, String>> convertToCloudEventAndHeadersMap(KafkaRecord<Integer, String> message) {
        switch (executor.getProcessor().getType()) {
            case SOURCE:
                return Pair.of(
                        toSourceCloudEvent(message.getPayload(), message.getHeaders()),
                        Collections.emptyMap());
            case ERROR_HANDLER:
                return Pair.of(
                        toErrorHandlerCloudEvent(message.getPayload()),
                        toHeadersMap(message.getHeaders()));
            default:
                return Pair.of(
                        CloudEventUtils.decode(message.getPayload()),
                        toHeadersMap(message.getHeaders()));
        }
    }

    private CloudEvent toSourceCloudEvent(String event, Headers headers) {
        try {
            // JsonCloudEventData.wrap requires an empty JSON
            JsonNode payload = event == null ? mapper.createObjectNode() : mapper.readTree(event);

            CloudEventData data = JsonCloudEventData.wrap(payload);

            return wrapToCloudEvent(
                    String.format("%sSource", executor.getProcessor().getDefinition().getRequestedSource().getType()),
                    data,
                    toExtensionsMap(headers));
        } catch (JsonProcessingException e2) {
            LOG.error("JsonProcessingException when generating CloudEvent for '{}'", event, e2);
            throw new CloudEventDeserializationException("Failed to generate event map");
        }
    }

    private CloudEvent toErrorHandlerCloudEvent(String event) {
        try {
            // try to decode as cloud event
            return CloudEventUtils.decode(event);
        } catch (CloudEventDeserializationException e) {
            // if it fails (e.g. for connector errors) try wrapping it
            CloudEventData data = BytesCloudEventData.wrap(event.getBytes(StandardCharsets.UTF_8));
            return wrapToCloudEvent("RhoseError", data, Collections.emptyMap());
        }
    }

    private CloudEvent wrapToCloudEvent(String type, CloudEventData data, Map<String, String> extensions) {
        CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create(CLOUD_EVENT_SOURCE))
                .withType(type)
                .withData(data);

        extensions.forEach(cloudEventBuilder::withExtension);

        return cloudEventBuilder.build();
    }

    static String getOriginalEventId(CloudEvent cloudEvent, KafkaRecord<Integer, String> message) {
        if (cloudEvent != null) {
            return cloudEvent.getId();
        }
        if (message.getKey() != null) {
            return message.getKey().toString();
        }
        if (message.getTimestamp() != null) {
            return message.getTimestamp().toString();
        }
        return "unknown";
    }

    static Map<String, String> toHeadersMap(Headers headers) {
        Map<String, String> headersMap = new TreeMap<>();
        if (headers != null) {
            for (Header header : headers) {
                headersMap.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
            }
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
