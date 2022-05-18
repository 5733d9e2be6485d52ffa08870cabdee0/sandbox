package com.redhat.service.smartevents.executor;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.CloudEventDeserializationException;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;

import static com.redhat.service.smartevents.executor.CloudEventExtension.adjustExtensionName;

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

    @Incoming(EVENTS_IN_CHANNEL)
    public CompletionStage<Void> processEvent(final IncomingKafkaRecord<Integer, String> message) {
        try {
            String eventPayload = message.getPayload();
            CloudEvent cloudEvent;
            if (executor.getProcessor().getType() == ProcessorType.SOURCE) {
                cloudEvent = wrapToCloudEvent(eventPayload, message.getHeaders());
            } else {
                Map<String, String> headers = new HashMap<>();
                for (Header h : message.getHeaders().toArray()) {
                    headers.put(h.key().substring(3), new String(h.value())); // TODO: refactor
                }

                cloudEvent = CloudEventBuilder.v1() // TODO: refactor
                        .withData(message.getPayload().getBytes(StandardCharsets.UTF_8))
                        .withId(headers.getOrDefault("id", null))
                        .withSource(new URI(headers.getOrDefault("source", null)))
                        .withType(headers.getOrDefault("type", null))
                        .withDataSchema(headers.containsKey("dataschema") ? new URI(headers.get("dataschema")) : null)
                        .withSubject(headers.getOrDefault("subject", null))
                        .build();
            }
            executor.onEvent(cloudEvent);
        } catch (Exception e) {
            LOG.error("Processor with id '{}' on bridge '{}' failed to handle Event. The message is acked anyway.",
                    executor.getProcessor().getId(), executor.getProcessor().getBridgeId(), e);
        }
        return message.ack();
    }

    private CloudEvent wrapToCloudEvent(String event, Headers headers) {
        try {
            // JsonCloudEventData.wrap requires an empty JSON
            JsonNode payload = stringEventToJsonOrEmpty(event);
            CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withSource(URI.create(CLOUD_EVENT_SOURCE))
                    .withType(String.format("%sSource", executor.getProcessor().getDefinition().getRequestedSource().getType()))
                    .withData(JsonCloudEventData.wrap(payload));

            Map<String, Object> extensions = wrapHeadersToExtensionsMap(headers);
            for (Map.Entry<String, Object> kv : extensions.entrySet()) {
                // We currently support only String extensions
                cloudEventBuilder.withExtension(kv.getKey(), kv.getValue().toString());
            }
            return cloudEventBuilder.build();
        } catch (JsonProcessingException e2) {
            LOG.error("JsonProcessingException when generating CloudEvent for '{}'", event, e2);
            throw new CloudEventDeserializationException("Failed to generate event map");
        }
    }

    public static Map<String, Object> wrapHeadersToExtensionsMap(Headers kafkaHeaders) {
        Map<String, Object> resultMap = new HashMap<>();
        if (kafkaHeaders == null) {
            return resultMap;
        }

        for (Header kh : kafkaHeaders) {
            String cloudEventsExtensionName = adjustExtensionName(kh.key());
            String headerValue = new String(kh.value(), StandardCharsets.UTF_8);
            resultMap.put(cloudEventsExtensionName, headerValue);
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
