package com.redhat.service.smartevents.shard.operator;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.dto.ProcessorManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsProvider;
import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.utils.Constants;
import com.redhat.service.smartevents.shard.operator.utils.DeploymentSpecUtils;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class BridgeExecutorServiceImpl implements BridgeExecutorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeExecutorServiceImpl.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @Inject
    TemplateProvider templateProvider;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    GlobalConfigurationsProvider globalConfigurationsProvider;

    @Inject
    ManagerClient managerClient;

    @Override
    public void createBridgeExecutor(ProcessorDTO processorDTO) {
        final Namespace namespace = customerNamespaceProvider.fetchOrCreateCustomerNamespace(processorDTO.getCustomerId());

        BridgeExecutor expected = BridgeExecutor.fromDTO(processorDTO, namespace.getMetadata().getName(), executorImage);

        BridgeExecutor existing = kubernetesClient
                .resources(BridgeExecutor.class)
                .inNamespace(namespace.getMetadata().getName())
                .withName(BridgeExecutor.resolveResourceName(processorDTO.getId()))
                .get();

        if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            BridgeExecutor bridgeExecutor = kubernetesClient
                    .resources(BridgeExecutor.class)
                    .inNamespace(namespace.getMetadata().getName())
                    .createOrReplace(expected);

            // create or update the secrets for the bridgeExecutor
            createOrUpdateBridgeExecutorSecret(bridgeExecutor, processorDTO);
        } else {
            ManagedResourceStatus inferredStatus = existing.getStatus().inferManagedResourceStatus();
            // The Controller would have notified the Manager with PROVISIONING before it first started.
            if (inferredStatus == ManagedResourceStatus.PROVISIONING) {
                return;
            }
            LOGGER.info("BridgeExecutor '{}' already exists and is '{}'. Notifying manager that it is '{}'.",
                    processorDTO.getId(),
                    inferredStatus,
                    inferredStatus);
            ProcessorManagedResourceStatusUpdateDTO updateDTO = new ProcessorManagedResourceStatusUpdateDTO(processorDTO.getId(),
                    processorDTO.getCustomerId(),
                    processorDTO.getBridgeId(),
                    inferredStatus);
            managerClient.notifyProcessorStatusChange(updateDTO).subscribe().with(
                    success -> LOGGER.debug("Ready notification for BridgeExecutor '{}' has been sent to the manager successfully", processorDTO.getId()),
                    failure -> LOGGER.error("Failed to send updated status to Manager for entity of type '{}'", ProcessorDTO.class.getSimpleName(), failure));
        }
    }

    @Override
    public void deleteBridgeExecutor(ProcessorDTO processorDTO) {
        final String namespace = customerNamespaceProvider.resolveName(processorDTO.getCustomerId());
        final boolean bridgeDeleted =
                kubernetesClient
                        .resources(BridgeExecutor.class)
                        .inNamespace(namespace)
                        .delete(BridgeExecutor.fromDTO(processorDTO, namespace, executorImage));
        if (!bridgeDeleted) {
            // TODO: we might need to review this use case and have a manager to look at a queue of objects not deleted and investigate. Unfortunately the API does not give us a reason.
            LOGGER.warn("BridgeExecutor '{}' not deleted. Notifying manager that it has been deleted.", processorDTO.getId());
            ProcessorManagedResourceStatusUpdateDTO updateDTO =
                    new ProcessorManagedResourceStatusUpdateDTO(processorDTO.getId(), processorDTO.getCustomerId(), processorDTO.getBridgeId(), ManagedResourceStatus.DELETED);
            managerClient.notifyProcessorStatusChange(updateDTO).subscribe().with(
                    success -> LOGGER.debug("Deleted notification for BridgeExecutor '{}' has been sent to the manager successfully", processorDTO.getId()),
                    failure -> LOGGER.error("Failed to send updated status to Manager for entity of type '{}'", ProcessorDTO.class.getSimpleName(), failure));
        }
    }

    @Override
    public List<BridgeExecutor> fetchAllBridgeExecutor() {
        return kubernetesClient.resources(BridgeExecutor.class).inAnyNamespace().list().getItems();
    }
}
