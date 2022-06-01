package com.redhat.service.smartevents.processor;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationResult;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

public interface ProcessorCatalogService {

    boolean isConnector(ProcessorType type, String name);

    List<String> getActionsCatalog();

    List<String> getSourcesCatalog();

    JsonSchema getActionJsonSchema(String name);

    JsonSchema getSourceJsonSchema(String name);

    ValidationResult validate(String name, ProcessorType type, ObjectNode data);
}
