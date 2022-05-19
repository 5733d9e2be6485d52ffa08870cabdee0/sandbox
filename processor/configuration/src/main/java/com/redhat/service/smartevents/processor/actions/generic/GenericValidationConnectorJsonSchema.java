package com.redhat.service.smartevents.processor.actions.generic;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.GatewayValidator;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
public class GenericValidationConnectorJsonSchema implements GatewayValidator<Action> {


    @Inject
    ObjectMapper objectMapper;

    protected String getJsonSchemaString(String name) {
        InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(name);

        return new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    protected JsonSchema getJsonSchemaFromJsonNode(JsonNode jsonNode) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        return factory.getSchema(jsonNode);
    }

    @Override
    public ValidationResult isValid(Action action) {

        ObjectNode rawFile;
        try {

            // TODO need to parametrise this depending on the connector type
            // This file was copied from
            // https://github.com/bf2fc6cc711aee1a0c2a/cos-fleet-catalog-camel/blob/main/etc/kubernetes/manifests/base/connectors/connector-catalog-camel-social/slack_sink_0.1.json#L42
            String schemaString = getJsonSchemaString("slack_sink_0.1.json");
            rawFile = objectMapper.readValue(schemaString, ObjectNode.class);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Managed connector provides a file with the schema inside among other information
        JsonNode schemaNode = rawFile.findPath("schema");
        JsonSchema schema = getJsonSchemaFromJsonNode(schemaNode);

        schema.initializeValidators();
        ObjectNode parameters = action.getRawParameters();

        parameters.set("kafka_topic", new TextNode("dummytopic")); // hack as the topic will be set later

        Set<ValidationMessage> errors = schema.validate(parameters);

        if(!errors.isEmpty()) {
            String errorsString = errors.stream().map(Objects::toString).collect(Collectors.joining("|"));
            return new ValidationResult(false, errorsString);
        }

        return new ValidationResult(true);
    }

    @Override
    public String getType() {
        return "Generic";
    }
}
