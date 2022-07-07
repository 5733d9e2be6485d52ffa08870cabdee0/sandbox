package com.redhat.service.smartevents.manager;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorLifecycleException;
import com.redhat.service.smartevents.infra.models.EventBridgeSecret;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.vault.VaultService;
import com.redhat.service.smartevents.processor.ProcessorCatalogService;

import io.smallrye.mutiny.Uni;

import static com.redhat.service.smartevents.manager.SecretsService.emptyObjectNode;
import static com.redhat.service.smartevents.manager.SecretsService.mergeObjectNodes;

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

    @Override
    public void maskProcessor(Processor processor) {
        // deep copy existing definition since maskProcessorDefinition directly updates the received one
        ProcessorDefinition maskedDefinition = processor.getDefinition().deepCopy();
        Map<String, ObjectNode> newSecrets = maskProcessorDefinition(maskedDefinition);
        boolean newHasSecrets = !newSecrets.isEmpty();

        if (processor.hasSecrets() && !newHasSecrets) {
            // processor used to have secrets and now has none, so we must delete the existing secrets
            await(vaultService.delete(secretNameFor(processor)));
        } else if (newHasSecrets) {
            // otherwise we must create/update the existing secrets
            Map<String, String> serializedNewSecrets = serializeSecrets(newSecrets);
            EventBridgeSecret eventBridgeSecret = new EventBridgeSecret(secretNameFor(processor), serializedNewSecrets);
            vaultService.createOrReplace(eventBridgeSecret).await().atMost(Duration.ofSeconds(secretsManagerTimeout));
        }

        // update processor
        processor.setDefinition(maskedDefinition);
        processor.setHasSecrets(newHasSecrets);
    }

    protected Map<String, ObjectNode> maskProcessorDefinition(ProcessorDefinition definition) {
        Map<String, ObjectNode> secretsMap = new HashMap<>();
        if (definition.getRequestedAction() != null) {
            ObjectNode gatewaySecrets = maskGateway(definition.getRequestedAction());
            if (!gatewaySecrets.isEmpty()) {
                secretsMap.put("requestedAction", gatewaySecrets);
            }
        }
        if (definition.getRequestedSource() != null) {
            ObjectNode gatewaySecrets = maskGateway(definition.getRequestedSource());
            if (!gatewaySecrets.isEmpty()) {
                secretsMap.put("requestedSource", gatewaySecrets);
            }
        }
        if (definition.getResolvedAction() != null) {
            ObjectNode gatewaySecrets = maskGateway(definition.getResolvedAction());
            if (!gatewaySecrets.isEmpty()) {
                secretsMap.put("resolvedAction", gatewaySecrets);
            }
        }
        return secretsMap;
    }

    protected <T extends Gateway> ObjectNode maskGateway(T gateway) {
        List<String> passwordProps = gateway instanceof Action
                ? processorCatalogService.getActionPasswordProperties(gateway.getType())
                : processorCatalogService.getSourcePasswordProperties(gateway.getType());

        ObjectNode parameters = gateway.getParameters();
        ObjectNode secrets = emptyObjectNode();
        for (String passwordProperty : passwordProps) {
            if (parameters.has(passwordProperty)) {
                secrets.set(passwordProperty, parameters.get(passwordProperty));
                parameters.set(passwordProperty, emptyObjectNode());
            }
        }
        gateway.setParameters(parameters);

        return secrets;
    }

    @Override
    public ProcessorDefinition getUnmaskedProcessorDefinition(Processor processor) {
        if (!processor.hasSecrets()) {
            return processor.getDefinition().deepCopy();
        }

        // retrieve secrets from vaultService
        EventBridgeSecret eventBridgeSecret = await(vaultService.get(secretNameFor(processor)));
        Map<String, ObjectNode> secrets = deserializeSecrets(eventBridgeSecret.getValues());

        // deep copy existing definition since unmaskProcessorDefinition directly updates the received one
        ProcessorDefinition unmaskedDefinition = processor.getDefinition().deepCopy();
        unmaskProcessorDefinition(unmaskedDefinition, secrets);

        return unmaskedDefinition;
    }

    protected void unmaskProcessorDefinition(ProcessorDefinition definition, Map<String, ObjectNode> secrets) {
        if (definition.getRequestedAction() != null) {
            ObjectNode newGatewayParams = mergeObjectNodes(definition.getRequestedAction().getParameters(), secrets.get("requestedAction"));
            definition.getRequestedAction().setParameters(newGatewayParams);
        }
        if (definition.getRequestedSource() != null) {
            ObjectNode newGatewayParams = mergeObjectNodes(definition.getRequestedSource().getParameters(), secrets.get("requestedSource"));
            definition.getRequestedSource().setParameters(newGatewayParams);
        }
        if (definition.getResolvedAction() != null) {
            ObjectNode newGatewayParams = mergeObjectNodes(definition.getResolvedAction().getParameters(), secrets.get("resolvedAction"));
            definition.getResolvedAction().setParameters(newGatewayParams);
        }
    }

    private Map<String, String> serializeSecrets(Map<String, ObjectNode> secrets) {
        try {
            Map<String, String> serializedSecrets = new HashMap<>();
            for (Map.Entry<String, ObjectNode> entry : secrets.entrySet()) {
                serializedSecrets.put(entry.getKey(), mapper.writeValueAsString(entry.getValue()));
            }
            return serializedSecrets;
        } catch (JsonProcessingException e) {
            throw new ProcessorLifecycleException("Error while serializing secrets");
        }
    }

    private Map<String, ObjectNode> deserializeSecrets(Map<String, String> secrets) {
        try {
            Map<String, ObjectNode> deserializedSecrets = new HashMap<>();
            for (Map.Entry<String, String> entry : secrets.entrySet()) {
                deserializedSecrets.put(entry.getKey(), (ObjectNode) mapper.readTree(entry.getValue()));
            }
            return deserializedSecrets;
        } catch (ClassCastException | JsonProcessingException e) {
            throw new ProcessorLifecycleException("Error while deserializing secrets");
        }
    }

    private String secretNameFor(Processor processor) {
        return resourceNamesProvider.getProcessorSecretName(processor.getId());
    }

    private <T> T await(Uni<T> uni) {
        return uni.await().atMost(Duration.ofSeconds(secretsManagerTimeout));
    }

}
