package com.redhat.service.smartevents.manager;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorLifecycleException;
import com.redhat.service.smartevents.infra.models.EventBridgeSecret;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.vault.VaultService;
import com.redhat.service.smartevents.processor.ProcessorCatalogService;

@ApplicationScoped
public class SecretsServiceImpl implements SecretsService {

    @ConfigProperty(name = "secretsmanager.timeout-seconds")
    int secretsManagerTimeout;

    @Inject
    ObjectMapper mapper;
    @Inject
    ProcessorCatalogService processorCatalogService;
    @Inject
    ResourceNamesProvider resourceNamesProvider;
    @Inject
    VaultService vaultService;

    public static ObjectNode emptyObjectNode() {
        return new ObjectNode(JsonNodeFactory.instance);
    }

    @Override
    public ProcessorDefinition maskProcessorDefinition(String processorId, ProcessorDefinition definition) {
        try {
            Pair<ProcessorDefinition, Map<String, ObjectNode>> p = maskProcessorDefinition(definition);
            // ObjectNodes must be serialized because VaultService supports only Map<String, String>
            Map<String, String> serializedSecretsMap = new HashMap<>();
            for (Map.Entry<String, ObjectNode> entry : p.getRight().entrySet()) {
                serializedSecretsMap.put(entry.getKey(), mapper.writeValueAsString(entry.getValue()));
            }

            if (!serializedSecretsMap.isEmpty()) {
                EventBridgeSecret eventBridgeSecret = new EventBridgeSecret(resourceNamesProvider.getProcessorSecretName(processorId), serializedSecretsMap);
                vaultService.createOrReplace(eventBridgeSecret).await().atMost(Duration.ofSeconds(secretsManagerTimeout));
            }

            return p.getLeft();
        } catch (JsonProcessingException e) {
            throw new ProcessorLifecycleException("Error while storing secrets");
        }
    }

    private Pair<ProcessorDefinition, Map<String, ObjectNode>> maskProcessorDefinition(ProcessorDefinition definition) {
        ProcessorDefinition definitionCopy = definition.deepCopy();

        Map<String, ObjectNode> secretsMap = new HashMap<>();
        if (definition.getRequestedAction() != null) {
            Pair<Action, ObjectNode> p = maskGateway(definition.getRequestedAction());
            definitionCopy.setRequestedAction(p.getLeft());
            if (!p.getRight().isEmpty()) {
                secretsMap.put("requestedAction", p.getRight());
            }
        }
        if (definition.getRequestedSource() != null) {
            Pair<Source, ObjectNode> p = maskGateway(definition.getRequestedSource());
            definitionCopy.setRequestedSource(p.getLeft());
            if (!p.getRight().isEmpty()) {
                secretsMap.put("requestedSource", p.getRight());
            }
        }
        if (definition.getResolvedAction() != null) {
            Pair<Action, ObjectNode> p = maskGateway(definition.getResolvedAction());
            definitionCopy.setResolvedAction(p.getLeft());
            if (!p.getRight().isEmpty()) {
                secretsMap.put("resolvedAction", p.getRight());
            }
        }
        return Pair.of(definitionCopy, secretsMap);
    }

    @Override
    public ProcessorDefinition unmaskProcessorDefinition(String processorId, ProcessorDefinition existingDefinition, ProcessorDefinition requestedDefinition) {
        try {
            EventBridgeSecret existingSecrets = vaultService.get(resourceNamesProvider.getProcessorSecretName(processorId))
                    .await().atMost(Duration.ofSeconds(secretsManagerTimeout));

            // ObjectNodes must be serialized because VaultService supports only Map<String, String>
            Map<String, ObjectNode> deserializedSecretsMap = new HashMap<>();
            for (Map.Entry<String, String> entry : existingSecrets.getValues().entrySet()) {
                deserializedSecretsMap.put(entry.getKey(), (ObjectNode) mapper.readTree(entry.getValue()));
            }
            return unmaskProcessorDefinition(existingDefinition, deserializedSecretsMap, requestedDefinition);
        } catch (ClassCastException | JsonProcessingException e) {
            throw new ProcessorLifecycleException("Error while retrieving secrets");
        }
    }

    private ProcessorDefinition unmaskProcessorDefinition(ProcessorDefinition existingDefinition, Map<String, ObjectNode> existingSecrets, ProcessorDefinition requestedDefinition) {
        if (existingDefinition == null) {
            return existingDefinition;
        }
        ProcessorDefinition definitionCopy = existingDefinition.deepCopy();

        if (existingDefinition.getRequestedAction() != null) {
            ObjectNode requestedParams = Optional.ofNullable(requestedDefinition)
                    .map(ProcessorDefinition::getRequestedAction)
                    .map(Gateway::getParameters)
                    .orElseGet(SecretsServiceImpl::emptyObjectNode);
            ObjectNode newGatewayParams = mergeNewerParams(existingSecrets.get("requestedAction"), requestedParams);
            definitionCopy.getRequestedAction().mergeParameters(newGatewayParams);
        }
        if (existingDefinition.getRequestedSource() != null) {
            ObjectNode requestedParams = Optional.ofNullable(requestedDefinition)
                    .map(ProcessorDefinition::getRequestedSource)
                    .map(Gateway::getParameters)
                    .orElseGet(SecretsServiceImpl::emptyObjectNode);
            ObjectNode newGatewayParams = mergeNewerParams(existingSecrets.get("requestedSource"), requestedParams);
            definitionCopy.getRequestedSource().mergeParameters(newGatewayParams);
        }
        if (existingDefinition.getResolvedAction() != null) {
            ObjectNode requestedParams = Optional.ofNullable(requestedDefinition)
                    .map(ProcessorDefinition::getResolvedAction)
                    .map(Gateway::getParameters)
                    .orElseGet(SecretsServiceImpl::emptyObjectNode);
            ObjectNode newGatewayParams = mergeNewerParams(existingSecrets.get("resolvedAction"), requestedParams);
            definitionCopy.getResolvedAction().mergeParameters(newGatewayParams);
        }
        return definitionCopy;
    }

    private static ObjectNode mergeNewerParams(ObjectNode existingParams, ObjectNode requestedParams) {
        Iterator<Map.Entry<String, JsonNode>> parametersIterator = requestedParams.fields();
        ObjectNode empty = emptyObjectNode();
        ObjectNode merged = existingParams != null ? existingParams.deepCopy() : emptyObjectNode();
        while (parametersIterator.hasNext()) {
            Map.Entry<String, JsonNode> parameterEntry = parametersIterator.next();
            if (!empty.equals(parameterEntry.getValue())) {
                merged.set(parameterEntry.getKey(), parameterEntry.getValue());
            }
        }
        return merged;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Gateway> Pair<T, ObjectNode> maskGateway(T gateway) {
        List<String> passwordProps = gateway instanceof Action
                ? processorCatalogService.getActionPasswordProperties(gateway.getType())
                : processorCatalogService.getSourcePasswordProperties(gateway.getType());

        T gatewayCopy = (T) gateway.deepCopy();
        ObjectNode parameters = gatewayCopy.getParameters();
        ObjectNode secrets = emptyObjectNode();
        for (String passwordProperty : passwordProps) {
            if (parameters.has(passwordProperty)) {
                secrets.set(passwordProperty, parameters.get(passwordProperty));
                parameters.set(passwordProperty, emptyObjectNode());
            }
        }
        gatewayCopy.setParameters(parameters);

        return Pair.of(gatewayCopy, secrets);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Gateway> T unmaskGateway(T gateway, ObjectNode secrets) {
        Iterator<Map.Entry<String, JsonNode>> it = secrets.fields();
        T gatewayCopy = (T) gateway.deepCopy();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> secretEntry = it.next();
            if (!gatewayCopy.getParameters().has(secretEntry.getKey()) || emptyObjectNode().equals(gatewayCopy.getParameters().get(secretEntry.getKey()))) {
                gatewayCopy.setParameter(secretEntry.getKey(), secretEntry.getValue());
            }
        }
        return gatewayCopy;
    }

}
