package com.redhat.service.bridge.infra.utils;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.infra.utils.exceptions.CloudEventDeserializationException;
import com.redhat.service.bridge.infra.utils.exceptions.CloudEventSerializationException;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import io.cloudevents.jackson.JsonFormat;

public class CloudEventUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CloudEventUtils.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(JsonFormat.getCloudEventJacksonModule());

    public static CloudEvent build(String id, String topic, URI source, String subject, JsonNode data) {
        CloudEventBuilder builder = null;
        builder = CloudEventBuilder.v1()
                .withId(id)
                .withSource(source)
                .withType(JsonNode.class.getName())
                .withExtension("topic", topic)
                .withData(JsonCloudEventData.wrap(data));

        if (subject != null) {
            builder.withSubject(subject);
        }

        return builder.build();
    }

    public static CloudEvent build(String id, String topic, URI source, String subject, CloudEvent data) {
        try {
            return build(id, topic, source, subject, OBJECT_MAPPER.readTree(encode(data)));
        } catch (JsonProcessingException e) {
            throw new CloudEventDeserializationException("Failed to parse cloud event to wrap");
        }
    }

    public static String encode(CloudEvent event) {
        try {
            return OBJECT_MAPPER.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            LOG.error("Unable to encode CloudEvent", e);
            throw new CloudEventSerializationException("Failed to encode CloudEvent");
        }
    }

    public static CloudEvent decode(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, CloudEvent.class);
        } catch (JsonProcessingException e) {
            LOG.error("Unable to decode CloudEvent", e);
            throw new CloudEventDeserializationException("Failed to decode Cloud Event");
        }
    }

    private CloudEventUtils() {
        throw new IllegalStateException("Instantiation of utility class CloudEventUtils is forbidden");
    }
}
