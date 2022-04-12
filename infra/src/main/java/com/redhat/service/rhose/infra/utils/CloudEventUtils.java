package com.redhat.service.rhose.infra.utils;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.rhose.infra.exceptions.definitions.user.CloudEventDeserializationException;
import com.redhat.service.rhose.infra.exceptions.definitions.user.CloudEventSerializationException;

import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import io.cloudevents.jackson.JsonFormat;

public class CloudEventUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CloudEventUtils.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(JsonFormat.getCloudEventJacksonModule());

    public static CloudEventBuilder builderFor(String id, SpecVersion specVersion, URI source, String subject, JsonNode data) {
        CloudEventBuilder builder = CloudEventBuilder.fromSpecVersion(specVersion)
                .withId(id)
                .withSource(source)
                .withType(JsonNode.class.getName())
                .withData(JsonCloudEventData.wrap(data));

        if (subject != null) {
            builder.withSubject(subject);
        }

        return builder;
    }

    public static CloudEvent build(String id, SpecVersion specVersion, URI source, String subject, JsonNode data) {
        return builderFor(id, specVersion, source, subject, data).build();
    }

    public static CloudEvent build(String id, SpecVersion specVersion, URI source, String subject, CloudEvent data) {
        try {
            return build(id, specVersion, source, subject, OBJECT_MAPPER.readTree(encode(data)));
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

    public static ObjectMapper getMapper() {
        return OBJECT_MAPPER;
    }

    private CloudEventUtils() {
        throw new IllegalStateException("Instantiation of utility class CloudEventUtils is forbidden");
    }
}
