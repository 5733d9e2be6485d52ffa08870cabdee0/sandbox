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
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.CloudEventDeserializationException;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;

@ApplicationScoped
public class ExecutorsService {

    /**
     * Channel used for receiving events.
     */
    public static final String EVENTS_IN_CHANNEL = "events-in";

    public static final String CLOUD_EVENT_SOURCE = "RHOSE";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorsService.class);

    @Inject
    Executor executor;

    @Inject
    ObjectMapper mapper;

    @Incoming(EVENTS_IN_CHANNEL)
    public CompletionStage<Void> processBridgeEvent(final IncomingKafkaRecord<Integer, String> message) {

        try {
            String eventPayload = message.getPayload();
            // TODO: REDO
            CloudEvent cloudEvent;
            if (executor.getProcessor().getType() == ProcessorType.SOURCE) {
                cloudEvent = wrapToCloudEvent(eventPayload);
            } else {
                Map<String, String> headers = new HashMap<>();
                for (Header h : message.getHeaders().toArray()) {
                    headers.put(h.key().substring(3), new String(h.value())); // TODO: refactor
                }

                cloudEvent = CloudEventBuilder.v1() // TODO: refactor
                        .withData(message.getPayload().getBytes(StandardCharsets.UTF_8))
                        .withId(headers.get("id"))
                        .withSource(new URI(headers.get("source")))
                        .withType(headers.get("type"))
                        .withDataSchema(new URI(headers.get("dataschema")))
                        .withSubject(headers.get("subject"))
                        .build();
            }
            executor.onEvent(cloudEvent);
        } catch (Exception e) {
            LOG.error("Processor with id '{}' on bridge '{}' failed to handle Event. The message is acked anyway.",
                    executor.getProcessor().getId(), executor.getProcessor().getBridgeId(), e);
        }
        return message.ack();
    }

    private CloudEvent wrapToCloudEvent(String event) {
        try {
            return CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withSource(URI.create(CLOUD_EVENT_SOURCE))
                    .withType(String.format("%sSource", executor.getProcessor().getDefinition().getRequestedSource().getType()))
                    .withData(JsonCloudEventData.wrap(mapper.readTree(event)))
                    .build();
        } catch (JsonProcessingException e2) {
            LOG.error("JsonProcessingException when generating CloudEvent for '{}'", event, e2);
            throw new CloudEventDeserializationException("Failed to generate event map");
        }
    }
}
