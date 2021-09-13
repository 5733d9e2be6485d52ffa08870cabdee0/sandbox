package com.redhat.service.bridge.ingress;

import java.net.URI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;

public class TestUtils {
    public static CloudEvent buildTestCloudEvent() throws JsonProcessingException {
        String jsonString = "{\"k1\":\"v1\",\"k2\":\"v2\"}";
        return CloudEventUtils.build("myId", "myTopic", URI.create("mySource"), "subject", new ObjectMapper().readTree(jsonString));
    }
}
