package com.redhat.service.bridge.shard.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.k8s.Action;
import com.redhat.service.bridge.infra.k8s.K8SBridgeConstants;
import com.redhat.service.bridge.infra.k8s.KubernetesClient;
import com.redhat.service.bridge.infra.k8s.KubernetesResourceType;
import com.redhat.service.bridge.infra.k8s.ResourceEvent;
import com.redhat.service.bridge.infra.k8s.crds.ProcessorCustomResource;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.shard.ManagerSyncService;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;

@ApplicationScoped
public class ProcessorController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorController.class);

    @Inject
    ManagerSyncService managerSyncService;

    @Inject
    KubernetesClient kubernetesClient;

    void onEvent(@Observes ResourceEvent event) { // equivalent of ResourceEventSource for operator sdk
        if (event.getSubject().equals(K8SBridgeConstants.PROCESSOR_TYPE)) {

            /*
             * If the CRD is deleted, remove also all the other related resource
             *
             * If another dependent resource is deleted, the `reconcileExecutor` will catch the mismatch between the expected state and the current state.
             * It will redeploy the resources so to reach the expected status at the end.
             */
            if (event.getAction().equals(Action.DELETED) && event.getResourceType().equals(KubernetesResourceType.CUSTOM_RESOURCE)) {
                delete(event.getResourceId());
                return;
            }

            ProcessorCustomResource resource = kubernetesClient.getCustomResource(event.getResourceId(), ProcessorCustomResource.class);
            if (event.getAction().equals(Action.ERROR)) {
                notifyFailedDeployment(resource);
                return;
            }

            reconcileExecutor(resource);
        }
    }

    private void delete(String resourceId) {
        // Delete Deployment
        kubernetesClient.deleteDeployment(resourceId);
    }

    private void reconcileExecutor(ProcessorCustomResource customResource) {
        LOGGER.info("[shard] Processor reconcyle loop called");

        String id = customResource.getId();

        // Create Executor Deployment if it does not exists
        Deployment deployment = kubernetesClient.getDeployment(id);
        if (deployment == null) {
            LOGGER.info("[shard] There is no deployment for Processor '{}'. Creating.", id);
            kubernetesClient.createOrUpdateDeployment(createExecutorDeployment(id, customResource.getBridge().getId()));
            return;
        }

        Optional<DeploymentCondition> optStatus = deployment.getStatus().getConditions().stream().filter(x -> x.getStatus().equals("Ready")).findFirst();

        if (!optStatus.isPresent()) {
            LOGGER.info("[shard] Executor deployment for Processor '{}' is not ready yet", id);
            return;
        }

        // TODO: Create service for deployment

        // Update the custom resource if needed
        if (!customResource.getStatus().equals(BridgeStatus.FAILED) && !customResource.getStatus().equals(BridgeStatus.AVAILABLE)) {
            customResource.setStatus(BridgeStatus.AVAILABLE);

            kubernetesClient.createOrUpdateCustomResource(customResource.getId(), customResource, K8SBridgeConstants.PROCESSOR_TYPE);

            LOGGER.info(customResource.getStatus().toString());
            ProcessorDTO dto = customResource.toDTO();
            managerSyncService.notifyProcessorStatusChange(dto).subscribe().with(
                    success -> LOGGER.info("[shard] Updating Processor with id '{}' done", dto.getId()),
                    failure -> LOGGER.warn("[shard] Updating Processor with id '{}' FAILED", dto.getId()));
        }
    }

    private Deployment createExecutorDeployment(String id, String bridgeId) {
        Map<String, String> labels = new HashMap<>();
        labels.put(K8SBridgeConstants.METADATA_TYPE, K8SBridgeConstants.PROCESSOR_TYPE);
        labels.put(K8SBridgeConstants.METADATA_BRIDGE_ID, bridgeId);

        return new DeploymentBuilder() // TODO: Add kind, replicas, image etc.. Or even read it from a yaml
                .withMetadata(new ObjectMetaBuilder()
                        .withName(id)
                        .withLabels(labels)
                        .build())
                .build();
    }

    private void notifyFailedDeployment(ProcessorCustomResource resource) {
        if (!resource.getStatus().equals(BridgeStatus.FAILED)) {
            LOGGER.error("[shard] Failed to deploy Deployment with id '{}'", resource.getId());
            resource.setStatus(BridgeStatus.FAILED);
            kubernetesClient.createOrUpdateCustomResource(resource.getId(), resource, K8SBridgeConstants.PROCESSOR_TYPE);
            ProcessorDTO dto = resource.toDTO();
            managerSyncService.notifyProcessorStatusChange(dto).subscribe().with(
                    success -> LOGGER.info("[shard] Updating Processor with id '{}' about the failure. Done", dto.getId()),
                    failure -> LOGGER.warn("[shard] Updating Processor with id '{}' about the failure. FAILED", dto.getId()));
        }
    }
}
