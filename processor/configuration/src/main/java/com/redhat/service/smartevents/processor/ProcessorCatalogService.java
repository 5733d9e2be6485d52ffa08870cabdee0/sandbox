package com.redhat.service.smartevents.processor;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationResult;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.processor.models.ProcessorCatalogEntry;

public interface ProcessorCatalogService {

    boolean isConnector(ProcessorType type, String name);

    List<ProcessorCatalogEntry> getActionsCatalog();

    List<ProcessorCatalogEntry> getSourcesCatalog();

    JsonSchema getActionJsonSchema(String id);

    JsonSchema getSourceJsonSchema(String id);

    List<String> getActionPasswordProperties(String id);

    List<String> getSourcePasswordProperties(String id);

    ValidationResult validate(String name, ProcessorType type, ObjectNode data);
}
