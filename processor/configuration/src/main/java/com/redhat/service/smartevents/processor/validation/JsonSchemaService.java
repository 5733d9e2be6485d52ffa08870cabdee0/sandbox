package com.redhat.service.smartevents.processor.validation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;

@ApplicationScoped
public class JsonSchemaService {

    @Inject
    ObjectMapper objectMapper;

    protected String getJsonSchemaString(String name) {
        InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(name);

        if (is == null) {
            throw new RuntimeException("Cannot find a json schema file for connector " + jsonFileName(name));
        }

        return new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    private String jsonFileName(String name) {
        return name + ".json";
    }

    protected JsonSchema getJsonSchemaFromJsonNode(JsonNode jsonNode) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        return factory.getSchema(jsonNode);
    }

    /**
     Find the Json Schema file from connector given the connector name.
     For example, a connector called "slack_sink_0.1" will return the Json Schema stored in the slack_sink_0.1.json
     The files are originally copied from MC repository i.e.
     https://github.com/bf2fc6cc711aee1a0c2a/cos-fleet-catalog-camel/blob/main/etc/kubernetes/manifests/base/connectors/connector-catalog-camel-social/slack_sink_0.1.json#L42
     */
    public JsonSchema findJsonSchemaForConnectorName(String connectorName) {
        ObjectNode rawFile;
        try {

            String jsonConnectorName = jsonFileName(connectorName);
            String schemaString = getJsonSchemaString(jsonConnectorName);

            rawFile = objectMapper.readValue(schemaString, ObjectNode.class);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Managed connector provides a file with the schema inside among other information
        JsonNode schemaNode = rawFile.findPath("schema");
        return getJsonSchemaFromJsonNode(schemaNode);
    }
}
