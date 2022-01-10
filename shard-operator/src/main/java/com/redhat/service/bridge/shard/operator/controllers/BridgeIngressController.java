package com.redhat.service.bridge.shard.operator.controllers;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.shard.operator.BridgeIngressService;
import com.redhat.service.bridge.shard.operator.ManagerSyncService;
import com.redhat.service.bridge.shard.operator.monitoring.ServiceMonitorService;
import com.redhat.service.bridge.shard.operator.networking.NetworkResource;
import com.redhat.service.bridge.shard.operator.networking.NetworkingService;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.resources.ConditionMessages;
import com.redhat.service.bridge.shard.operator.resources.ConditionReason;
import com.redhat.service.bridge.shard.operator.resources.ConditionType;
import com.redhat.service.bridge.shard.operator.utils.Constants;
import com.redhat.service.bridge.shard.operator.watchers.DeploymentEventSource;
import com.redhat.service.bridge.shard.operator.watchers.ServiceEventSource;
import com.redhat.service.bridge.shard.operator.watchers.monitoring.ServiceMonitorEventSource;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
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

    @Inject
    ServiceMonitorService monitorService;

    @Override
    public void init(EventSourceManager eventSourceManager) {
        DeploymentEventSource deploymentEventSource = DeploymentEventSource.createAndRegisterWatch(kubernetesClient, BridgeIngress.COMPONENT_NAME);
        eventSourceManager.registerEventSource("bridge-ingress-deployment-event-source", deploymentEventSource);
        ServiceEventSource serviceEventSource = ServiceEventSource.createAndRegisterWatch(kubernetesClient, BridgeIngress.COMPONENT_NAME);
        eventSourceManager.registerEventSource("bridge-ingress-service-event-source", serviceEventSource);
        AbstractEventSource networkingEventSource = networkingService.createAndRegisterWatchNetworkResource(BridgeIngress.COMPONENT_NAME);
        eventSourceManager.registerEventSource("bridge-ingress-networking-event-source", networkingEventSource);
        Optional<ServiceMonitorEventSource> serviceMonitorEventSource = ServiceMonitorEventSource.createAndRegisterWatch(kubernetesClient, BridgeIngress.COMPONENT_NAME);
        serviceMonitorEventSource.ifPresent(monitorEventSource -> eventSourceManager.registerEventSource("bridge-ingress-monitoring-event-source", monitorEventSource));
    }

    @Override
    public UpdateControl<BridgeIngress> createOrUpdateResource(BridgeIngress bridgeIngress, Context<BridgeIngress> context) {
        LOGGER.debug("Create or update BridgeIngress: '{}' in namespace '{}'", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());

        Deployment deployment = bridgeIngressService.fetchOrCreateBridgeIngressDeployment(bridgeIngress);

        if (!Readiness.isDeploymentReady(deployment)) {
            LOGGER.debug("Ingress deployment BridgeIngress: '{}' in namespace '{}' is NOT ready", bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace());

            // TODO: notify the manager if in FailureState: .status.Type = Ready and .status.Reason = DeploymentFailed

            bridgeIngress.getStatus().setConditionsFromDeployment(deployment);
            return UpdateControl.updateStatusSubResource(bridgeIngress);
        }
        LOGGER.debug("Ingress deployment BridgeIngress: '{}' in namespace '{}' is ready", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());

        // Create Service
        Service service = bridgeIngressService.fetchOrCreateBridgeIngressService(bridgeIngress, deployment);
        if (service.getStatus() == null) {
            LOGGER.debug("Ingress service BridgeIngress: '{}' in namespace '{}' is NOT ready", bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace());
            bridgeIngress.getStatus().markConditionFalse(ConditionType.Ready);
            bridgeIngress.getStatus().markConditionTrue(ConditionType.Augmentation, ConditionReason.ServiceNotReady);
            return UpdateControl.updateStatusSubResource(bridgeIngress);
        }
        LOGGER.debug("Ingress service BridgeIngress: '{}' in namespace '{}' is ready", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());

        // Create Route
        NetworkResource networkResource = networkingService.fetchOrCreateNetworkIngress(bridgeIngress, service);

        if (!networkResource.isReady()) {
            LOGGER.debug("Ingress networking resource BridgeIngress: '{}' in namespace '{}' is NOT ready", bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace());
            bridgeIngress.getStatus().markConditionFalse(ConditionType.Ready);
            bridgeIngress.getStatus().markConditionTrue(ConditionType.Augmentation, ConditionReason.NetworkResourceNotReady);
            return UpdateControl.updateStatusSubResource(bridgeIngress);
        }
        LOGGER.debug("Ingress networking resource BridgeIngress: '{}' in namespace '{}' is ready", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());

        Optional<ServiceMonitor> serviceMonitor = monitorService.fetchOrCreateServiceMonitor(bridgeIngress, service, BridgeIngress.COMPONENT_NAME);
        if (serviceMonitor.isPresent()) {
            // this is an optional resource
            LOGGER.debug("Ingress monitor resource BridgeIngress: '{}' in namespace '{}' is ready", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());
        } else {
            LOGGER.warn("Ingress monitor resource BridgeIngress: '{}' in namespace '{}' is failed to deploy, Prometheus not installed.", bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace());
            bridgeIngress.getStatus().markConditionFalse(ConditionType.Ready, ConditionReason.PrometheusUnavailable, ConditionMessages.PROMETHEUS_UNVAILABLE);
            notifyManager(bridgeIngress, BridgeStatus.FAILED);
            return UpdateControl.updateStatusSubResource(bridgeIngress);
        }

        if (!bridgeIngress.getStatus().isReady() || !networkResource.getEndpoint().equals(bridgeIngress.getStatus().getEndpoint())) {
            bridgeIngress.getStatus().setEndpoint(networkResource.getEndpoint());
            bridgeIngress.getStatus().markConditionTrue(ConditionType.Ready);
            bridgeIngress.getStatus().markConditionFalse(ConditionType.Augmentation);
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

        managerSyncService.notifyBridgeStatusChange(dto)
                .onFailure().retry().atMost(Constants.MAX_HTTP_RETRY)
                .subscribe().with(
                        success -> LOGGER.info("[shard] Updating Bridge with id '{}' done", dto.getId()),
                        failure -> LOGGER.error("[shard] Updating Bridge with id '{}' FAILED", dto.getId()));
    }
}
