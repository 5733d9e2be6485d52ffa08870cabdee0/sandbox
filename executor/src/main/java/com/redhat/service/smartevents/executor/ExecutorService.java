package com.redhat.service.smartevents.executor;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.DeserializationException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.CloudEventDeserializationException;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.BytesCloudEventData;
import io.cloudevents.jackson.JsonCloudEventData;
import io.cloudevents.kafka.CloudEventDeserializer;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

import static com.redhat.service.smartevents.executor.CloudEventExtension.adjustExtensionName;
import static com.redhat.service.smartevents.infra.api.APIConstants.RHOSE_BRIDGE_ID_HEADER;
import static com.redhat.service.smartevents.infra.api.APIConstants.RHOSE_ERROR_CODE_HEADER;
import static com.redhat.service.smartevents.infra.api.APIConstants.RHOSE_ORIGINAL_EVENT_ID_HEADER;
import static com.redhat.service.smartevents.infra.api.APIConstants.RHOSE_ORIGINAL_EVENT_SOURCE_HEADER;
import static com.redhat.service.smartevents.infra.api.APIConstants.RHOSE_ORIGINAL_EVENT_SUBJECT_HEADER;
import static com.redhat.service.smartevents.infra.api.APIConstants.RHOSE_ORIGINAL_EVENT_TYPE_HEADER;
import static com.redhat.service.smartevents.infra.api.APIConstants.RHOSE_PROCESSOR_ID_HEADER;

@ApplicationScoped
public class ExecutorService {

    /**
     * Channel used for receiving events.
     */
    public static final String EVENTS_IN_CHANNEL = "events-in";

    public static final String CLOUD_EVENT_SOURCE = "RHOSE";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorService.class);

    private static final List<String> TRACE_HEADERS = List.of(
            RHOSE_BRIDGE_ID_HEADER,
            RHOSE_PROCESSOR_ID_HEADER,
            RHOSE_ORIGINAL_EVENT_SOURCE_HEADER,
            RHOSE_ORIGINAL_EVENT_ID_HEADER,
            RHOSE_ORIGINAL_EVENT_TYPE_HEADER,
            RHOSE_ORIGINAL_EVENT_SUBJECT_HEADER,
            RHOSE_ERROR_CODE_HEADER);

    @ConfigProperty(name = "mp.messaging.incoming.events-in.topic")
    String topic;

    @Inject
    Executor executor;

    @Inject
    ObjectMapper mapper;

    @Inject
    BridgeErrorService bridgeErrorService;

    @Incoming(EVENTS_IN_CHANNEL)
    public CompletionStage<Void> processEvent(final IncomingKafkaRecord<Integer, String> message) {
        CloudEvent cloudEvent = null;
        try {
            Pair<CloudEvent, Map<String, String>> pair = convertToCloudEventAndHeadersMap(message);
            cloudEvent = pair.getLeft();

            // add trace headers map for SOURCE and SINK processors
            Map<String, String> fullHeadersMap = executor.getProcessor().getType() == ProcessorType.ERROR_HANDLER
                    ? pair.getRight()
                    : Stream.concat(pair.getRight().entrySet().stream(), getTraceHeadersMap(cloudEvent, message).entrySet().stream())
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, TreeMap::new));

            executor.onEvent(pair.getLeft(), fullHeadersMap);
        } catch (Exception e) {
            LOG.error("Processor with id '{}' on bridge '{}' failed to handle Event.",
                    executor.getProcessor().getId(), executor.getProcessor().getBridgeId(), e);

            // create trace headers value map
            Map<String, String> traceHeadersMap = getTraceHeadersMap(cloudEvent, message);
            bridgeErrorService.getError(e).ifPresent(error -> traceHeadersMap.put(RHOSE_ERROR_CODE_HEADER, error.getCode()));

            // Add our Kafka Headers, first removing any pre-existing ones to avoid duplication.
            // This can be replaced with w3c trace-context parameters when we add distributed tracing.
            Headers headers = message.getHeaders() == null ? new RecordHeaders() : message.getHeaders();
            TRACE_HEADERS.forEach(headers::remove);
            traceHeadersMap.forEach((key, value) -> headers.add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8))));

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
                return buildCloudEvent(message, true);
            default:
                return buildCloudEvent(message, false);
        }
    }

    private Pair<CloudEvent, Map<String, String>> buildCloudEvent(KafkaRecord<Integer, String> message, boolean wrapOnFailure) {
        try (CloudEventDeserializer cloudEventDeserializer = new CloudEventDeserializer()) {
            CloudEvent cloudEvent = cloudEventDeserializer
                    .deserialize(topic, message.getHeaders(), message.getPayload().getBytes(StandardCharsets.UTF_8));
            return Pair.of(
                    cloudEvent,
                    toHeadersMap(message.getHeaders()));
        } catch (Exception e) {
            // if it fails (e.g. for connector errors) try wrapping it
            if (wrapOnFailure) {
                CloudEventData data = BytesCloudEventData.wrap(message.getPayload().getBytes(StandardCharsets.UTF_8));
                return Pair.of(wrapToCloudEvent("RhoseError", data, Collections.emptyMap()), toHeadersMap(message.getHeaders()));
            }
            throw new DeserializationException("Failed to deserialize the cloud event", e);
        }
    }

    private CloudEvent toSourceCloudEvent(String event, Headers headers) {
        try {
            // JsonCloudEventData.wrap requires an empty JSON
            JsonNode payload = event == null ? mapper.createObjectNode() : mapper.readTree(event);

            CloudEventData data = JsonCloudEventData.wrap(payload);

            return wrapToCloudEvent(
                    String.format("%s", executor.getProcessor().getDefinition().getRequestedSource().getType()),
                    data,
                    toExtensionsMap(headers));
        } catch (JsonProcessingException e2) {
            LOG.error("JsonProcessingException when generating CloudEvent for '{}'", event, e2);
            throw new CloudEventDeserializationException("Failed to generate event map");
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

    private Map<String, String> getTraceHeadersMap(CloudEvent cloudEvent, KafkaRecord<Integer, String> message) {
        String originalSourceHeader = cloudEvent != null ? cloudEvent.getSource().toString() : CLOUD_EVENT_SOURCE;
        String originalIdHeader = getOriginalEventId(cloudEvent, message);

        Map<String, String> traceHeaders = new TreeMap<>();
        traceHeaders.put(RHOSE_BRIDGE_ID_HEADER, executor.getProcessor().getBridgeId());
        traceHeaders.put(RHOSE_PROCESSOR_ID_HEADER, executor.getProcessor().getId());
        traceHeaders.put(RHOSE_ORIGINAL_EVENT_SOURCE_HEADER, originalSourceHeader);
        traceHeaders.put(RHOSE_ORIGINAL_EVENT_ID_HEADER, originalIdHeader);
        if (cloudEvent != null) {
            if (cloudEvent.getType() != null) {
                traceHeaders.put(RHOSE_ORIGINAL_EVENT_TYPE_HEADER, cloudEvent.getType());
            }
            if (cloudEvent.getSubject() != null) {
                traceHeaders.put(RHOSE_ORIGINAL_EVENT_SUBJECT_HEADER, cloudEvent.getSubject());
            }
        }
        return traceHeaders;
    }

    static String getOriginalEventId(CloudEvent cloudEvent, KafkaRecord<Integer, String> message) {
        if (cloudEvent != null) {
            return cloudEvent.getId();
        }
        if (message.getKey() != null) {
            return message.getKey().toString();
        }
        LOG.error("Can't find original event ID for message {}", message);
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
