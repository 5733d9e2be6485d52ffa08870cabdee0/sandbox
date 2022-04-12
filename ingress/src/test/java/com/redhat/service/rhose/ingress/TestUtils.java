package com.redhat.service.rhose.ingress;

import java.net.URI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.rhose.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.builder.CloudEventBuilder;

public class TestUtils {

    public static final String DEFAULT_BRIDGE_ID = "bridgeId";

    public static CloudEvent buildTestCloudEvent() throws JsonProcessingException {
        return builderForTestCloudEvent().build();
    }

    private static CloudEventBuilder builderForTestCloudEvent() throws JsonProcessingException {
        String jsonString = "{\"k1\":\"v1\",\"k2\":\"v2\"}";
        return CloudEventUtils.builderFor("myId", SpecVersion.V1, URI.create("mySource"), "subject", new ObjectMapper().readTree(jsonString));
    }
}
