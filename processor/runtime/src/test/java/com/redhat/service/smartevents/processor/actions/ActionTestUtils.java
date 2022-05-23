package com.redhat.service.smartevents.processor.actions;

import java.net.URI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.smartevents.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;

public class ActionTestUtils {

    public static final String PLAIN_EVENT_JSON = "{\"key\":\"value\"}";

    public static final URI CLOUD_EVENT_SOURCE = URI.create("mySource");
    public static final String CLOUD_EVENT_TYPE = "TestEvent";
    public static final String CLOUD_EVENT_ID = "myId";
    public static final String CLOUD_EVENT_SUBJECT = "subject";

    public static CloudEvent createCloudEvent() {
        try {
            JsonNode data = CloudEventUtils.getMapper().readTree(PLAIN_EVENT_JSON);
            return CloudEventUtils.builderFor(CLOUD_EVENT_ID, SpecVersion.V1, CLOUD_EVENT_SOURCE, CLOUD_EVENT_SUBJECT, CLOUD_EVENT_TYPE, data)
                    .withType(CLOUD_EVENT_TYPE)
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
