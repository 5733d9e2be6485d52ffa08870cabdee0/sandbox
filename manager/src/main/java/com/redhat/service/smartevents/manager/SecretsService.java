package com.redhat.service.smartevents.manager;

import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.manager.models.Processor;

public interface SecretsService {

    static ObjectNode emptyObjectNode() {
        return new ObjectNode(JsonNodeFactory.instance);
    }

    static ProcessorDefinition mergeDefinitions(ProcessorDefinition existingDefinition, ProcessorDefinition newDefinition) {
        if (newDefinition == null) {
            return existingDefinition;
        }

        ProcessorDefinition mergedDefinition = existingDefinition != null ? existingDefinition.deepCopy() : new ProcessorDefinition();
        mergedDefinition.setFilters(newDefinition.getFilters());
        mergedDefinition.setTransformationTemplate(newDefinition.getTransformationTemplate());

        if (newDefinition.getRequestedAction() != null) {
            ObjectNode currentParams = mergedDefinition.getRequestedAction() != null ? mergedDefinition.getRequestedAction().getParameters() : null;
            Action mergedAction = newDefinition.getRequestedAction().deepCopy();
            mergedAction.setParameters(mergeObjectNodes(currentParams, newDefinition.getRequestedAction().getParameters()));
            mergedDefinition.setRequestedAction(mergedAction);
        }

        if (newDefinition.getRequestedSource() != null) {
            ObjectNode currentParams = mergedDefinition.getRequestedSource() != null ? mergedDefinition.getRequestedSource().getParameters() : null;
            Source mergedSource = newDefinition.getRequestedSource().deepCopy();
            mergedSource.setParameters(mergeObjectNodes(currentParams, newDefinition.getRequestedSource().getParameters()));
            mergedDefinition.setRequestedSource(mergedSource);
        }

        if (newDefinition.getResolvedAction() != null) {
            ObjectNode currentParams = mergedDefinition.getResolvedAction() != null ? mergedDefinition.getResolvedAction().getParameters() : null;
            Action mergedAction = newDefinition.getResolvedAction().deepCopy();
            mergedAction.setParameters(mergeObjectNodes(currentParams, newDefinition.getResolvedAction().getParameters()));
            mergedDefinition.setResolvedAction(mergedAction);
        }

        return mergedDefinition;
    }

    static ObjectNode mergeObjectNodes(ObjectNode existingNode, ObjectNode newNode) {
        if (newNode == null) {
            return existingNode;
        }

        ObjectNode mergedValues = existingNode != null ? existingNode.deepCopy() : emptyObjectNode();
        Iterator<Map.Entry<String, JsonNode>> parametersIterator = newNode.fields();

        while (parametersIterator.hasNext()) {
            Map.Entry<String, JsonNode> parameterEntry = parametersIterator.next();
            JsonNode parameterNode = parameterEntry.getValue();
            if (!(parameterNode.isObject() && parameterNode.isEmpty())) {
                mergedValues.set(parameterEntry.getKey(), parameterEntry.getValue());
            }
        }

        return mergedValues;
    }

    void maskProcessor(Processor processor);

    ProcessorDefinition getUnmaskedProcessorDefinition(Processor processor);

}
