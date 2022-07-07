package com.redhat.service.smartevents.manager;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;

public interface SecretsService {

    ProcessorDefinition maskProcessorDefinition(String processorId, ProcessorDefinition definition);

    ProcessorDefinition unmaskProcessorDefinition(String processorId, ProcessorDefinition existingDefinition, ProcessorDefinition requestedDefinition);

    <T extends Gateway> Pair<T, ObjectNode> maskGateway(T gateway);

    <T extends Gateway> T unmaskGateway(T gateway, ObjectNode secrets);
}
