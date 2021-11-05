package com.redhat.service.bridge.shard.operator.controllers;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.shard.operator.BridgeIngressService;
import com.redhat.service.bridge.shard.operator.ManagerSyncService;
import com.redhat.service.bridge.shard.operator.networking.NetworkResource;
import com.redhat.service.bridge.shard.operator.networking.NetworkingService;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngressStatus;
import com.redhat.service.bridge.shard.operator.resources.PhaseType;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;
import com.redhat.service.bridge.shard.operator.watchers.DeploymentEventSource;
import com.redhat.service.bridge.shard.operator.watchers.ServiceEventSource;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;

/**
 * To be implemented on <a href="https://issues.redhat.com/browse/MGDOBR-93">MGDOBR-93</a>
 */
@ApplicationScoped
@Controller
public class BridgeIngressController implements ResourceController<BridgeIngress> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeIngressController.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ManagerSyncService managerSyncService;

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    NetworkingService networkingService;

    @Override
    public void init(EventSourceManager eventSourceManager) {
        DeploymentEventSource deploymentEventSource = DeploymentEventSource.createAndRegisterWatch(kubernetesClient, LabelsBuilder.BRIDGE_INGRESS_COMPONENT);
        eventSourceManager.registerEventSource("bridge-ingress-deployment-event-source", deploymentEventSource);
        ServiceEventSource serviceEventSource = ServiceEventSource.createAndRegisterWatch(kubernetesClient, LabelsBuilder.BRIDGE_INGRESS_COMPONENT);
        eventSourceManager.registerEventSource("bridge-ingress-service-event-source", serviceEventSource);
        AbstractEventSource networkingEventSource = networkingService.createAndRegisterWatchNetworkResource(LabelsBuilder.BRIDGE_INGRESS_COMPONENT);
        eventSourceManager.registerEventSource("bridge-ingress-networking-event-source", networkingEventSource);
    }

    @Override
    public UpdateControl<BridgeIngress> createOrUpdateResource(BridgeIngress bridgeIngress, Context<BridgeIngress> context) {
        LOGGER.info("Create or update BridgeIngress: '{}' in namespace '{}'", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());

        Deployment deployment = bridgeIngressService.fetchOrCreateBridgeIngressDeployment(bridgeIngress);

        if (!Readiness.isDeploymentReady(deployment)) {
            LOGGER.info("Ingress deployment BridgeIngress: '{}' in namespace '{}' is NOT ready", bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace());

            // TODO: Check if the deployment is in an error state, update the CRD and notify the manager!

            bridgeIngress.setStatus(new BridgeIngressStatus(PhaseType.AUGMENTATION));
            return UpdateControl.updateStatusSubResource(bridgeIngress);
        }
        LOGGER.info("Ingress deployment BridgeIngress: '{}' in namespace '{}' is ready", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());

        // Create Service
        Service service = bridgeIngressService.getOrCreateBridgeIngressService(bridgeIngress, deployment);
        if (service.getStatus() == null) {
            LOGGER.info("Ingress service BridgeIngress: '{}' in namespace '{}' is NOT ready", bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace());
            bridgeIngress.setStatus(new BridgeIngressStatus(PhaseType.AUGMENTATION));
            return UpdateControl.updateStatusSubResource(bridgeIngress);
        }
        LOGGER.info("Ingress service BridgeIngress: '{}' in namespace '{}' is ready", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());

        // Create Route
        NetworkResource networkResource = networkingService.fetchOrCreateNetworkIngress(service);

        if (!networkResource.isReady()) {
            LOGGER.info("Ingress networking resource BridgeIngress: '{}' in namespace '{}' is NOT ready", bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace());
            bridgeIngress.setStatus(new BridgeIngressStatus(PhaseType.AUGMENTATION));
            return UpdateControl.updateStatusSubResource(bridgeIngress);
        }

        // Extract Route and populate the CRD. Notify the manager.

        if (!PhaseType.AVAILABLE.equals(bridgeIngress.getStatus().getPhase())) {
            bridgeIngress.setStatus(new BridgeIngressStatus(PhaseType.AVAILABLE));
            notifyManager(bridgeIngress, BridgeStatus.AVAILABLE);
            return UpdateControl.updateStatusSubResource(bridgeIngress);
        }
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl deleteResource(BridgeIngress bridgeIngress, Context<BridgeIngress> context) {
        LOGGER.info("Deleted BridgeIngress: '{}' in namespace '{}'", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());

        // Linked resources are automatically deleted

        return DeleteControl.DEFAULT_DELETE;
    }

    private void notifyManager(BridgeIngress bridgeIngress, BridgeStatus status) {
        BridgeDTO dto = bridgeIngress.toDTO();
        dto.setStatus(status);

        managerSyncService.notifyBridgeStatusChange(dto).subscribe().with(
                success -> LOGGER.info("[shard] Updating Bridge with id '{}' done", dto.getId()),
                failure -> LOGGER.warn("[shard] Updating Bridge with id '{}' FAILED", dto.getId()));
    }
}
