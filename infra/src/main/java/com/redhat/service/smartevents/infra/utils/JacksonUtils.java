package com.redhat.service.smartevents.infra.utils;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JacksonUtils {

    // TODO probably there's a better way than passing through a String
    public static ObjectNode mapToObjectNode(Map<String, String> parametersMap) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            String parameterString = mapper.writeValueAsString(parametersMap);
            return mapper.readValue(parameterString, ObjectNode.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
