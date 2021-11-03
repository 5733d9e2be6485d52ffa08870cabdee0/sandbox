package com.redhat.service.bridge.shard.operator.controllers;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.shard.operator.BridgeIngressService;
import com.redhat.service.bridge.shard.operator.ManagerSyncService;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngressStatus;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;
import com.redhat.service.bridge.shard.operator.watchers.DeploymentEventSource;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;

/**
 * To be implemented on <a href="https://issues.redhat.com/browse/MGDOBR-93">MGDOBR-93</a>
 */
@ApplicationScoped
@Controller
public class BridgeIngressController implements ResourceController<BridgeIngress> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeIngressController.class);

    // TODO: not using now, but let it here to make sure our configuration is ok and the object is being injected.
    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ManagerSyncService managerSyncService;

    @Inject
    BridgeIngressService bridgeIngressService;

    @Override
    public void init(EventSourceManager eventSourceManager) {
        DeploymentEventSource deploymentEventSource = DeploymentEventSource.createAndRegisterWatch(kubernetesClient, LabelsBuilder.BRIDGE_INGRESS_APPLICATION_TYPE);
        eventSourceManager.registerEventSource("bridge-ingress-deployment-event-source", deploymentEventSource);
    }

    @Override
    public UpdateControl<BridgeIngress> createOrUpdateResource(BridgeIngress bridgeIngress, Context<BridgeIngress> context) {
        // simplistic reconciliation to check with IT
        LOGGER.info("Create or update BridgeIngress: '{}' in namespace '{}'", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());

        Deployment deployment = bridgeIngressService.fetchOrCreateBridgeIngressDeployment(bridgeIngress);

        if (!Readiness.isDeploymentReady(deployment)) {
            LOGGER.info("Ingress deployment BridgeIngress: '{}' in namespace '{}' is NOT ready", bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace());
            return UpdateControl.noUpdate();
        }

        LOGGER.info("Ingress deployment BridgeIngress: '{}' in namespace '{}' is ready", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());

        // Deploy Service

        // Deploy Route

        // Extract Route and populate the CRD. Notify the manager.

        if (bridgeIngress.getStatus() == null) {
            bridgeIngress.setStatus(new BridgeIngressStatus());
        }

        if (bridgeIngress.getStatus().getStatus() == null || bridgeIngress.getStatus().getStatus().isEmpty()) {
            bridgeIngress.getStatus().setStatus("OK");
            notifyManager(bridgeIngress, BridgeStatus.AVAILABLE);
            return UpdateControl.updateStatusSubResource(bridgeIngress);
        }
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl deleteResource(BridgeIngress bridgeIngress, Context<BridgeIngress> context) {
        LOGGER.info("Deleted BridgeIngress: '{}' in namespace '{}'", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());

        // Linked resources are automatically deleted

        notifyManager(bridgeIngress, BridgeStatus.DELETED);

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
