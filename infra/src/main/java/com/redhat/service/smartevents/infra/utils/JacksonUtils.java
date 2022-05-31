package com.redhat.service.smartevents.infra.utils;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.InternalPlatformException;

public class JacksonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // TODO probably there's a better way than passing through a String
    public static ObjectNode mapToObjectNode(Map<String, String> parametersMap) {
        try {
            String parameterString = MAPPER.writeValueAsString(parametersMap);
            return MAPPER.readValue(parameterString, ObjectNode.class);
        } catch (Exception e) {
            throw new InternalPlatformException("Could not transform map to ObjectNode", e);
        }
    }
}
