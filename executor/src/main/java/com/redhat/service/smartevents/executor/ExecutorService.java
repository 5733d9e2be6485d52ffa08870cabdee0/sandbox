package com.redhat.service.smartevents.executor;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.exceptions.BridgeError;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.CloudEventDeserializationException;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
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
     * Channel used for receiving events.
     */
    public static final String EVENTS_IN_CHANNEL = "events-in";

    public static final String CLOUD_EVENT_SOURCE = "RHOSE";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorService.class);

    private static Set<String> RHOSE_HEADERS = Set.of(
            APIConstants.X_RHOSE_BRIDGE_ID,
            APIConstants.X_RHOSE_PROCESSOR_ID,
            APIConstants.X_RHOSE_ORIGINAL_EVENT_ID,
            APIConstants.X_RHOSE_ERROR_CODE);

    @Inject
    Executor executor;

    @Inject
    ObjectMapper mapper;

    @Inject
    BridgeErrorService bridgeErrorService;

    @Incoming(EVENTS_IN_CHANNEL)
    public CompletionStage<Void> processEvent(final KafkaRecord<Integer, String> message) {
        String eventPayload;
        CloudEvent cloudEvent = null;
        eventPayload = message.getPayload();
        Headers headers = message.getHeaders();
        Map<String, String> traceHeaders = getTraceHeaders(headers);

        try {

            // Unwrap Event
            ProcessorType type = executor.getProcessor().getType();
            if (type == ProcessorType.SOURCE) {
                // Source processors only handle non-Cloud Events
                cloudEvent = wrapToCloudEventWithExtensions(eventPayload, headers, getGateway());
            } else {
                // Sink processors can possibly handle both types of event. Unfortunately we're
                // unable to ascertain the nature of the payload therefore, if the payload cannot
                // be de-serialised as a Cloud Event, fallback to a wrapper.
                try {
                    cloudEvent = CloudEventUtils.decode(eventPayload);
                    cloudEvent = wrapToCloudEventWithExtensions(cloudEvent, headers);
                } catch (CloudEventDeserializationException e) {
                    cloudEvent = wrapToCloudEventWithExtensions(eventPayload, headers, getGateway());
                }
            }

            executor.onEvent(cloudEvent, traceHeaders);
        } catch (Exception e) {
            LOG.error("Processor with id '{}' on bridge '{}' failed to handle Event.",
                    executor.getProcessor().getId(), executor.getProcessor().getBridgeId(), e);
            String bridgeId = executor.getProcessor().getBridgeId();
            String processorId = executor.getProcessor().getId();
            String originalEventId = Objects.nonNull(cloudEvent) ? cloudEvent.getId() : message.getKey().toString();

            // Add our Kafka Headers, first removing any pre-existing ones to avoid duplication.
            // This can be replaced with w3c trace-context parameters when we add distributed tracing.
            headers.remove(APIConstants.X_RHOSE_BRIDGE_ID);
            headers.remove(APIConstants.X_RHOSE_PROCESSOR_ID);
            headers.remove(APIConstants.X_RHOSE_ORIGINAL_EVENT_ID);
            headers.remove(APIConstants.X_RHOSE_ERROR_CODE);
            headers.add(new RecordHeader(APIConstants.X_RHOSE_BRIDGE_ID, bridgeId.getBytes(StandardCharsets.UTF_8)));
            headers.add(new RecordHeader(APIConstants.X_RHOSE_PROCESSOR_ID, processorId.getBytes(StandardCharsets.UTF_8)));
            headers.add(new RecordHeader(APIConstants.X_RHOSE_ORIGINAL_EVENT_ID, originalEventId.getBytes(StandardCharsets.UTF_8)));

            // Add RHOSE's error code, if applicable
            Optional<BridgeError> be = bridgeErrorService.getError(e);
            be.ifPresent(err -> headers.add(new RecordHeader(APIConstants.X_RHOSE_ERROR_CODE,
                    err.getCode().getBytes(StandardCharsets.UTF_8))));

            return message.nack(e,
                    Metadata.of(OutgoingKafkaRecordMetadata.builder().withHeaders(new RecordHeaders(headers)).build()));
        }

        return message.ack();
    }

    private Map<String, String> getTraceHeaders(Headers headers) {
        Map<String, String> traceHeaders = new HashMap<>();
        for (Header header : headers.toArray()) {
            if (RHOSE_HEADERS.contains(header.key())) {
                traceHeaders.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
            }
        }
        return traceHeaders;
    }

    private Gateway getGateway() {
        ProcessorType type = executor.getProcessor().getType();
        if (type == ProcessorType.SOURCE) {
            return executor.getProcessor().getDefinition().getRequestedSource();
        } else {
            return executor.getProcessor().getDefinition().getRequestedAction();
        }
    }

    private CloudEvent wrapToCloudEventWithExtensions(String event, Headers headers, Gateway gateway) {
        try {
            // JsonCloudEventData.wrap requires an empty JSON
            JsonNode payload = stringEventToJsonOrEmpty(event);
            CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withSource(URI.create(CLOUD_EVENT_SOURCE))
                    .withType(String.format("%sSource", gateway.getType()))
                    .withData(JsonCloudEventData.wrap(payload));
            addKafkaHeadersAsCloudEventExtensions(headers, cloudEventBuilder);
            return cloudEventBuilder.build();
        } catch (JsonProcessingException e2) {
            LOG.error("JsonProcessingException when generating CloudEvent for '{}'", event, e2);
            throw new CloudEventDeserializationException("Failed to generate event map");
        }
    }

    // When we receive a CloudEvent on the ErrorHandler Processor Kafka has added DLQ headers
    // including the error details. Make sure we add them into the CloudEvent that is then sent
    // somewhere else by whatever ActionInvoker.
    private CloudEvent wrapToCloudEventWithExtensions(CloudEvent event, Headers headers) {
        CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                .withId(event.getId())
                .withSource(event.getSource())
                .withType(event.getType())
                .withData(event.getData());
        addKafkaHeadersAsCloudEventExtensions(headers, cloudEventBuilder);
        return cloudEventBuilder.build();
    }

    private void addKafkaHeadersAsCloudEventExtensions(Headers headers, CloudEventBuilder cloudEventBuilder) {
        Map<String, Object> extensions = wrapHeadersToExtensionsMap(headers);
        for (Map.Entry<String, Object> kv : extensions.entrySet()) {
            // We currently support only String extensions
            cloudEventBuilder.withExtension(kv.getKey(), kv.getValue().toString());
        }
    }

    // Add all Kafka Record Headers to Cloud Event extensions, excluding RHOSEs headers.
    public static Map<String, Object> wrapHeadersToExtensionsMap(Headers kafkaHeaders) {
        Map<String, Object> resultMap = new HashMap<>();
        if (kafkaHeaders == null) {
            return resultMap;
        }

        for (Header kh : kafkaHeaders) {
            if (!RHOSE_HEADERS.contains(kh.key())) {
                String cloudEventsExtensionName = adjustExtensionName(kh.key());
                String headerValue = new String(kh.value(), StandardCharsets.UTF_8);
                resultMap.put(cloudEventsExtensionName, headerValue);
            }
        }
        return resultMap;
    }

    private JsonNode stringEventToJsonOrEmpty(String event) throws JsonProcessingException {
        JsonNode payload;
        if (event == null) {
            payload = mapper.createObjectNode();
        } else {
            payload = mapper.readTree(event);
        }
        return payload;
    }
}
