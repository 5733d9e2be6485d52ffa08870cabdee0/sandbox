package com.redhat.service.smartevents.processor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationResult;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.DeserializationException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.processor.models.ProcessorCatalogEntry;

@ApplicationScoped
public class ProcessorCatalogServiceImpl implements ProcessorCatalogService {

    Logger LOGGER = LoggerFactory.getLogger(ProcessorCatalogServiceImpl.class);

    private static final String ACTIONS_DIR_PATH = "/schemas/actions/";
    private static final String SOURCES_DIR_PATH = "/schemas/sources/";
    private static final String JSON_FILE_EXTENSION = ".json";
    private static final String CATALOG_FILENAME = "catalog";

    private List<ProcessorCatalogEntry> actions;
    private List<ProcessorCatalogEntry> sources;

    @Inject
    ObjectMapper mapper;

    @PostConstruct
    void init() throws IOException {
        actions = mapper.readValue(readFile(ACTIONS_DIR_PATH, CATALOG_FILENAME), new TypeReference<List<ProcessorCatalogEntry>>() {
        });
        sources = mapper.readValue(readFile(SOURCES_DIR_PATH, CATALOG_FILENAME), new TypeReference<List<ProcessorCatalogEntry>>() {
        });
    }

    @Override
    public boolean isConnector(ProcessorType type, String id) {
        Optional<ProcessorCatalogEntry> entry = Optional.empty();
        if (ProcessorType.SOURCE.equals(type)) {
            entry = sources.stream().filter(x -> x.getId().equals(id)).findFirst();
        }
        if (ProcessorType.SINK.equals(type)) {
            entry = actions.stream().filter(x -> x.getId().equals(id)).findFirst();
        }

        if (entry.isEmpty()) {
            throw new ItemNotFoundException(String.format("Processor with id '%s' and type '%s' was not found in the catalog", id, type));
        }

        LOGGER.info("++++++ Catalog entry {}  ", entry);

        return entry.get().isConnector();
    }

    @Override
    public List<ProcessorCatalogEntry> getActionsCatalog() {
        return actions;
    }

    @Override
    public List<ProcessorCatalogEntry> getSourcesCatalog() {
        return sources;
    }

    @Override
    public JsonSchema getActionJsonSchema(String id) {
        return getJsonSchemaFromJsonNode(readActionJsonSchema(id));
    }

    @Override
    public JsonSchema getSourceJsonSchema(String id) {
        return getJsonSchemaFromJsonNode(readSourceJsonSchema(id));
    }

    @Override
    public ValidationResult validate(String id, ProcessorType type, ObjectNode data) {
        if (ProcessorType.SOURCE.equals(type)) {
            return getSourceJsonSchema(id).validateAndCollect(data);
        }
        if (ProcessorType.SINK.equals(type)) {
            return getActionJsonSchema(id).validateAndCollect(data);
        }
        throw new ItemNotFoundException(String.format("Processor type '%s' not recognized", type));
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

    private ObjectNode readActionJsonSchema(String id) {
        try {
            return mapper.readValue(readFile(ACTIONS_DIR_PATH, id), ObjectNode.class);
        } catch (JsonProcessingException e) {
            throw new DeserializationException(String.format("Could not deserialize the json schema '%s'", id), e);
        }
    }

    private ObjectNode readSourceJsonSchema(String id) {
        try {
            return mapper.readValue(readFile(SOURCES_DIR_PATH, id), ObjectNode.class);
        } catch (JsonProcessingException e) {
            throw new DeserializationException(String.format("Could not deserialize the json schema '%s'", id), e);
        }
    }

    private JsonSchema getJsonSchemaFromJsonNode(ObjectNode objectNode) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        return factory.getSchema(objectNode);
    }
}
