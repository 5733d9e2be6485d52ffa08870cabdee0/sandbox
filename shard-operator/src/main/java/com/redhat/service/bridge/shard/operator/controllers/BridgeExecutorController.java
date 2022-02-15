package com.redhat.service.bridge.shard.operator.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.exceptions.BridgeError;
import com.redhat.service.bridge.infra.exceptions.BridgeErrorService;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.PrometheusNotInstalledException;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.shard.operator.BridgeExecutorService;
import com.redhat.service.bridge.shard.operator.ManagerSyncService;
import com.redhat.service.bridge.shard.operator.monitoring.ServiceMonitorService;
import com.redhat.service.bridge.shard.operator.resources.BridgeExecutor;
import com.redhat.service.bridge.shard.operator.resources.ConditionReason;
import com.redhat.service.bridge.shard.operator.resources.ConditionType;
import com.redhat.service.bridge.shard.operator.utils.DeploymentStatusUtils;
import com.redhat.service.bridge.shard.operator.utils.EventSourceFactory;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

@ApplicationScoped
@ControllerConfiguration
public class BridgeExecutorController implements Reconciler<BridgeExecutor>,
        EventSourceInitializer<BridgeExecutor> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeExecutorController.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ManagerSyncService managerSyncService;

    @Inject
    BridgeExecutorService bridgeExecutorService;

    @Inject
    ServiceMonitorService monitorService;

    @Inject
    BridgeErrorService bridgeErrorService;

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<BridgeExecutor> eventSourceContext) {

        List<EventSource> eventSources = new ArrayList<>();
        eventSources.add(EventSourceFactory.buildSecretsInformer(kubernetesClient, BridgeExecutor.COMPONENT_NAME));
        eventSources.add(EventSourceFactory.buildDeploymentsInformer(kubernetesClient, BridgeExecutor.COMPONENT_NAME));
        eventSources.add(EventSourceFactory.buildServicesInformer(kubernetesClient, BridgeExecutor.COMPONENT_NAME));
        eventSources.add(EventSourceFactory.buildServicesMonitorInformer(kubernetesClient, BridgeExecutor.COMPONENT_NAME));

        return eventSources;
    }

    @Override
    public UpdateControl<BridgeExecutor> reconcile(BridgeExecutor bridgeExecutor, Context context) {
        LOGGER.debug("Create or update BridgeProcessor: '{}' in namespace '{}'", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());

        Secret secret = bridgeExecutorService.fetchBridgeExecutorSecret(bridgeExecutor);

        if (secret == null) {
            LOGGER.debug("Secrets for the BridgeProcessor '{}' have been not created yet.", bridgeExecutor.getMetadata().getName());
            return UpdateControl.noUpdate();
        }

        Deployment deployment = bridgeExecutorService.fetchOrCreateBridgeExecutorDeployment(bridgeExecutor, secret);
        if (!Readiness.isDeploymentReady(deployment)) {
            LOGGER.debug("Executor deployment BridgeProcessor: '{}' in namespace '{}' is NOT ready", bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());

            // TODO: notify the manager if in FailureState: .status.Type = Ready and .status.Reason = DeploymentFailed

            bridgeExecutor.getStatus().setConditionsFromDeployment(deployment);

            if (DeploymentStatusUtils.isTimeoutFailure(deployment)) {
                notifyDeploymentFailure(bridgeExecutor, DeploymentStatusUtils.getReasonAndMessageForTimeoutFailure(deployment));
            }

            return UpdateControl.updateStatus(bridgeExecutor);
        }
        LOGGER.debug("Executor deployment BridgeProcessor: '{}' in namespace '{}' is ready", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());

        // Create Service
        Service service = bridgeExecutorService.fetchOrCreateBridgeExecutorService(bridgeExecutor, deployment);
        if (service.getStatus() == null) {
            LOGGER.debug("Executor service BridgeProcessor: '{}' in namespace '{}' is NOT ready", bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());
            bridgeExecutor.getStatus().markConditionFalse(ConditionType.Ready);
            bridgeExecutor.getStatus().markConditionTrue(ConditionType.Augmentation, ConditionReason.ServiceNotReady);
            return UpdateControl.updateStatus(bridgeExecutor);
        }

        Optional<ServiceMonitor> serviceMonitor = monitorService.fetchOrCreateServiceMonitor(bridgeExecutor, service, BridgeExecutor.COMPONENT_NAME);
        if (serviceMonitor.isPresent()) {
            // this is an optional resource
            LOGGER.debug("Executor service monitor resource BridgeExecutor: '{}' in namespace '{}' is ready", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());
        } else {
            LOGGER.warn("Executor service monitor resource BridgeExecutor: '{}' in namespace '{}' is failed to deploy, Prometheus not installed.", bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());
            BridgeError prometheusNotAvailableError = bridgeErrorService.getError(PrometheusNotInstalledException.class)
                    .orElseThrow(() -> new RuntimeException("PrometheusNotInstalledException not found in error catalog"));
            bridgeExecutor.getStatus().markConditionFalse(ConditionType.Ready,
                    ConditionReason.PrometheusUnavailable,
                    prometheusNotAvailableError.getReason(),
                    prometheusNotAvailableError.getCode());
            notifyManager(bridgeExecutor, BridgeStatus.FAILED);
            return UpdateControl.updateStatus(bridgeExecutor);
        }

        LOGGER.debug("Executor service BridgeProcessor: '{}' in namespace '{}' is ready", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());

        if (!bridgeExecutor.getStatus().isReady()) {
            bridgeExecutor.getStatus().markConditionTrue(ConditionType.Ready);
            bridgeExecutor.getStatus().markConditionFalse(ConditionType.Augmentation);
            notifyManager(bridgeExecutor, BridgeStatus.AVAILABLE);
            return UpdateControl.updateStatus(bridgeExecutor);
        }
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl cleanup(BridgeExecutor bridgeExecutor, Context context) {
        LOGGER.info("Deleted BridgeProcessor: '{}' in namespace '{}'", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());

        // Linked resources are automatically deleted

        notifyManager(bridgeExecutor, BridgeStatus.DELETED);

        return DeleteControl.defaultDelete();
    }

    private void notifyDeploymentFailure(BridgeExecutor bridgeExecutor, String failureReason) {
        LOGGER.warn("Processor deployment BridgeIngress: '{}' in namespace '{}' has failed with reason: '{}'",
                bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace(), failureReason);
        notifyManager(bridgeExecutor, BridgeStatus.FAILED);
    }

    private void notifyManager(BridgeExecutor bridgeExecutor, BridgeStatus status) {
        ProcessorDTO dto = bridgeExecutor.toDTO();
        dto.setStatus(status);

        managerSyncService.notifyProcessorStatusChange(dto)
                .subscribe().with(
                        success -> LOGGER.info("Updating Processor with id '{}' done", dto.getId()),
                        failure -> LOGGER.error("Updating Processor with id '{}' FAILED", dto.getId()));
    }
}
