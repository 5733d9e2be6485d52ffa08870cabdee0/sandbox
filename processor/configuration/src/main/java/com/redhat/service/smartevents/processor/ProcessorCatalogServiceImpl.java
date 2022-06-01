package com.redhat.service.smartevents.processor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationResult;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

@ApplicationScoped
public class JsonSchemaServiceImpl implements JsonSchemaService {

    private static final String ACTIONS_DIR_PATH = "/schemas/actions/";
    private static final String SOURCES_DIR_PATH = "/schemas/sources/";
    private static final String JSON_FILE_EXTENSION = ".json";
    private static final String CATALOG_FILENAME = "catalog";

    private List<String> actions;
    private List<String> sources;

    @Inject
    ObjectMapper mapper;

    @PostConstruct
    void init() throws IOException {
        actions = mapper.readValue(readFile(ACTIONS_DIR_PATH, CATALOG_FILENAME), new TypeReference<>() {
        });
        sources = mapper.readValue(readFile(SOURCES_DIR_PATH, CATALOG_FILENAME), new TypeReference<>() {
        });
    }

    @Override
    public List<String> getActionsCatalog() {
        return actions;
    }

    @Override
    public List<String> getSourcesCatalog() {
        return sources;
    }

    @Override
    public JsonSchema getActionJsonSchema(String name) {
        return getJsonSchemaFromJsonNode(readActionJsonSchema(name));
    }

    @Override
    public JsonSchema getSourceJsonSchema(String name) {
        return getJsonSchemaFromJsonNode(readSourceJsonSchema(name));
    }

    @Override
    public ValidationResult validate(String name, ProcessorType type, ObjectNode data) {
        if (ProcessorType.SOURCE.equals(type)) {
            return getSourceJsonSchema(name).validateAndCollect(data);
        }
        return getActionJsonSchema(name).validateAndCollect(data);
    }

    private String readFile(String resourceDirectory, String name) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceDirectory + name + JSON_FILE_EXTENSION);

        if (is == null) {
            throw new ItemNotFoundException(String.format("Could not find '%s'.", name));
        }

        return new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));
    }

    private ObjectNode readActionJsonSchema(String name) {
        try {
            return mapper.readValue(readFile(ACTIONS_DIR_PATH, name), ObjectNode.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e); //TODO: raise specific exception
        }
    }

    private ObjectNode readSourceJsonSchema(String name) {
        try {
            return mapper.readValue(readFile(SOURCES_DIR_PATH, name), ObjectNode.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e); //TODO: raise specific exception
        }
    }

    private JsonSchema getJsonSchemaFromJsonNode(ObjectNode objectNode) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        return factory.getSchema(objectNode);
    }
}
