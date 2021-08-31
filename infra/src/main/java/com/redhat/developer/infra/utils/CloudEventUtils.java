package com.redhat.developer.infra.utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import io.cloudevents.jackson.JsonFormat;

public class CloudEventUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CloudEventUtils.class);

    public static final String UNKNOWN_SOURCE_URI_STRING = urlEncodedStringFrom("__UNKNOWN_SOURCE__")
            .orElseThrow(IllegalStateException::new);

    public static Optional<CloudEvent> build(String id, String topic, URI source, String type, String subject, JsonNode data) {
        CloudEventBuilder builder = CloudEventBuilder.v1()
                .withId(id)
                .withSource(source)
                .withType(type)
                .withExtension("topic", topic)
                .withData(JsonCloudEventData.wrap(data));

        if (subject != null) {
            builder.withSubject(subject);
        }

        return Optional.of(builder.build());
    }

    public static Optional<String> encode(CloudEvent event) {
        try {
            return Optional.of(Mapper.mapper().writeValueAsString(event));
        } catch (JsonProcessingException e) {
            LOG.error("Unable to encode CloudEvent", e);
            return Optional.empty();
        }
    }

    public static Optional<CloudEvent> decode(String json) {
        try {
            return Optional.of(Mapper.mapper().readValue(json, CloudEvent.class));
        } catch (JsonProcessingException e) {
            LOG.error("Unable to decode CloudEvent", e);
            return Optional.empty();
        }
    }

    public static Optional<String> urlEncodedStringFrom(String input) {
        return Optional.ofNullable(input)
                .map(i -> {
                    try {
                        return URLEncoder.encode(i, StandardCharsets.UTF_8.toString());
                    } catch (UnsupportedEncodingException e) {
                        LOG.error("Unable to URL-encode string \"" + i + "\"", e);
                        return null;
                    }
                });
    }

    public static Optional<URI> urlEncodedURIFrom(String input) {
        return urlEncodedStringFrom(input)
                .map(encodedInput -> {
                    try {
                        return URI.create(encodedInput);
                    } catch (IllegalArgumentException e) {
                        LOG.error("Unable to create URI from string \"" + encodedInput + "\"", e);
                        return null;
                    }
                });
    }

    // This trick allows to inject a mocked ObjectMapper in the unit tests via Mockito#mockStatic
    public static class Mapper {

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(JsonFormat.getCloudEventJacksonModule());

        public static ObjectMapper mapper() {
            return OBJECT_MAPPER;
        }

        private Mapper() {
            throw new IllegalStateException("Instantiation of utility class CloudEventUtils.Mapper is forbidden");
        }
    }

    private CloudEventUtils() {
        throw new IllegalStateException("Instantiation of utility class CloudEventUtils is forbidden");
    }
}
