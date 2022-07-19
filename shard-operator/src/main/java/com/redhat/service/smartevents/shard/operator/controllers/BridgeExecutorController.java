package com.redhat.service.smartevents.shard.operator.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.exceptions.BridgeError;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorHelper;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.PrometheusNotInstalledException;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.ProvisioningFailureException;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.BridgeExecutorService;
import com.redhat.service.smartevents.shard.operator.ManagerClient;
import com.redhat.service.smartevents.shard.operator.monitoring.ServiceMonitorService;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.ConditionReasonConstants;
import com.redhat.service.smartevents.shard.operator.resources.ConditionTypeConstants;
import com.redhat.service.smartevents.shard.operator.resources.CustomResourceStatus;
import com.redhat.service.smartevents.shard.operator.utils.DeploymentStatusUtils;
import com.redhat.service.smartevents.shard.operator.utils.EventSourceFactory;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.RetryInfo;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

@ApplicationScoped
@ControllerConfiguration(labelSelector = LabelsBuilder.RECONCILER_LABEL_SELECTOR)
public class BridgeExecutorController implements Reconciler<BridgeExecutor>,
        EventSourceInitializer<BridgeExecutor>, ErrorStatusHandler<BridgeExecutor> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeExecutorController.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ManagerClient managerClient;

    @Inject
    BridgeExecutorService bridgeExecutorService;

    @Inject
    ServiceMonitorService monitorService;

    @Inject
    BridgeErrorHelper bridgeErrorHelper;

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
        LOGGER.info("Create or update BridgeProcessor: '{}' in namespace '{}'", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());

        Secret secret = bridgeExecutorService.fetchBridgeExecutorSecret(bridgeExecutor);

        if (secret == null) {
            LOGGER.info("Secrets for the BridgeProcessor '{}' have been not created yet.", bridgeExecutor.getMetadata().getName());
            return UpdateControl.noUpdate();
        }

        // Check if the image of the executor has to be updated
        String image = bridgeExecutorService.getExecutorImage();
        if (!image.equals(bridgeExecutor.getSpec().getImage())) {
            bridgeExecutor.getSpec().setImage(image);
            return UpdateControl.updateResource(bridgeExecutor);
        }

        Deployment deployment = bridgeExecutorService.fetchOrCreateBridgeExecutorDeployment(bridgeExecutor, secret);
        if (!Readiness.isDeploymentReady(deployment)) {
            LOGGER.info("Executor deployment BridgeProcessor: '{}' in namespace '{}' is NOT ready", bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());

            setStatusFromDeployment(bridgeExecutor, deployment);

            if (DeploymentStatusUtils.isTimeoutFailure(deployment)) {
                String message = DeploymentStatusUtils.getReasonAndMessageForTimeoutFailure(deployment);
                BridgeError bridgeError = bridgeErrorHelper.getBridgeError(new ProvisioningFailureException(message));
                notifyManagerOfFailure(bridgeExecutor, bridgeError);
            } else if (DeploymentStatusUtils.isStatusReplicaFailure(deployment)) {
                String message = DeploymentStatusUtils.getReasonAndMessageForReplicaFailure(deployment);
                BridgeError bridgeError = bridgeErrorHelper.getBridgeError(new ProvisioningFailureException(message));
                notifyManagerOfFailure(bridgeExecutor, bridgeError);
            }

            return UpdateControl.updateStatus(bridgeExecutor);
        }
        LOGGER.info("Executor deployment BridgeProcessor: '{}' in namespace '{}' is ready", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());

        // Create Service
        Service service = bridgeExecutorService.fetchOrCreateBridgeExecutorService(bridgeExecutor, deployment);
        if (service.getStatus() == null) {
            LOGGER.info("Executor service BridgeProcessor: '{}' in namespace '{}' is NOT ready", bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());
            bridgeExecutor.getStatus().markConditionFalse(ConditionTypeConstants.READY);
            bridgeExecutor.getStatus().markConditionTrue(ConditionTypeConstants.AUGMENTATION, ConditionReasonConstants.SERVICE_NOT_READY);
            return UpdateControl.updateStatus(bridgeExecutor);
        }

        Optional<ServiceMonitor> serviceMonitor = monitorService.fetchOrCreateServiceMonitor(bridgeExecutor, service, BridgeExecutor.COMPONENT_NAME);
        if (serviceMonitor.isPresent()) {
            // this is an optional resource
            LOGGER.info("Executor service monitor resource BridgeExecutor: '{}' in namespace '{}' is ready", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());
        } else {
            LOGGER.warn("Executor service monitor resource BridgeExecutor: '{}' in namespace '{}' is failed to deploy, Prometheus not installed.", bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());
            BridgeError bridgeError = bridgeErrorHelper.getBridgeError(new PrometheusNotInstalledException(ConditionReasonConstants.PROMETHEUS_UNAVAILABLE));
            setStatusFromBridgeError(bridgeExecutor, bridgeError);
            notifyManagerOfFailure(bridgeExecutor, bridgeError);
            return UpdateControl.updateStatus(bridgeExecutor);
        }

        LOGGER.info("Executor service BridgeProcessor: '{}' in namespace '{}' is ready", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());

        if (!bridgeExecutor.getStatus().isReady()) {
            bridgeExecutor.getStatus().markConditionTrue(ConditionTypeConstants.READY);
            bridgeExecutor.getStatus().markConditionFalse(ConditionTypeConstants.AUGMENTATION);
            notifyManager(bridgeExecutor, ManagedResourceStatus.READY);
            return UpdateControl.updateStatus(bridgeExecutor);
        }
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl cleanup(BridgeExecutor bridgeExecutor, Context context) {
        LOGGER.info("Deleted BridgeProcessor: '{}' in namespace '{}'", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());

        // Linked resources are automatically deleted

        notifyManager(bridgeExecutor, ManagedResourceStatus.DELETED);

        return DeleteControl.defaultDelete();
    }

    @Override
    public Optional<BridgeExecutor> updateErrorStatus(BridgeExecutor bridgeExecutor, RetryInfo retryInfo, RuntimeException e) {
        if (retryInfo.isLastAttempt()) {
            BridgeError bridgeError = bridgeErrorHelper.getBridgeError(e);
            setStatusFromBridgeError(bridgeExecutor, bridgeError);
            notifyManagerOfFailure(bridgeExecutor, bridgeError);
        }
        return Optional.of(bridgeExecutor);
    }

    private void notifyManager(BridgeExecutor bridgeExecutor, ManagedResourceStatus status) {
        ProcessorDTO dto = bridgeExecutor.toDTO();
        dto.setStatus(status);

        managerClient.notifyProcessorStatusChange(dto)
                .subscribe().with(
                        success -> LOGGER.info("Updating Processor with id '{}' done", dto.getId()),
                        failure -> LOGGER.error("Updating Processor with id '{}' FAILED", dto.getId(), failure));
    }

    private void notifyManagerOfFailure(BridgeExecutor bridgeExecutor, BridgeError bridgeError) {
        LOGGER.error("Processor BridgeExecutor: '{}' in namespace '{}' has failed with reason: '{}'",
                bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace(), bridgeError.getReason());

        ProcessorDTO dto = bridgeExecutor.toDTO();
        dto.setStatus(ManagedResourceStatus.FAILED);

        managerClient.notifyProcessorFailure(dto, bridgeError)
                .subscribe().with(
                        success -> LOGGER.info("Updating Processor with id '{}' done", dto.getId()),
                        failure -> LOGGER.error("Updating Processor with id '{}' FAILED", dto.getId(), failure));
    }

    private void setStatusFromBridgeError(BridgeExecutor bridgeExecutor, BridgeError bridgeError) {
        CustomResourceStatus resourceStatus = bridgeExecutor.getStatus();
        resourceStatus.markConditionFalse(ConditionTypeConstants.READY,
                bridgeError.getReason(),
                bridgeError.getReason(),
                bridgeError.getCode());
        resourceStatus.markConditionFalse(ConditionTypeConstants.AUGMENTATION);
    }

    private void setStatusFromDeployment(BridgeExecutor bridgeExecutor, Deployment deployment) {
        CustomResourceStatus resourceStatus = bridgeExecutor.getStatus();
        if (deployment.getStatus() == null) {
            resourceStatus.markConditionFalse(ConditionTypeConstants.READY,
                    ConditionReasonConstants.DEPLOYMENT_NOT_AVAILABLE, "");
            resourceStatus.markConditionFalse(ConditionTypeConstants.AUGMENTATION);
        } else if (Readiness.isDeploymentReady(deployment)) {
            resourceStatus.markConditionTrue(ConditionTypeConstants.READY,
                    ConditionReasonConstants.DEPLOYMENT_AVAILABLE);
            resourceStatus.markConditionFalse(ConditionTypeConstants.AUGMENTATION);
        } else {
            if (DeploymentStatusUtils.isTimeoutFailure(deployment)) {
                resourceStatus.markConditionFalse(ConditionTypeConstants.READY,
                        ConditionReasonConstants.DEPLOYMENT_FAILED,
                        DeploymentStatusUtils.getReasonAndMessageForTimeoutFailure(deployment));
                resourceStatus.markConditionFalse(ConditionTypeConstants.AUGMENTATION);
            } else if (DeploymentStatusUtils.isStatusReplicaFailure(deployment)) {
                resourceStatus.markConditionFalse(ConditionTypeConstants.READY,
                        ConditionReasonConstants.DEPLOYMENT_FAILED,
                        DeploymentStatusUtils.getReasonAndMessageForReplicaFailure(deployment));
                resourceStatus.markConditionFalse(ConditionTypeConstants.AUGMENTATION);
            } else {
                resourceStatus.markConditionFalse(ConditionTypeConstants.READY,
                        ConditionReasonConstants.DEPLOYMENT_NOT_AVAILABLE, "");
                resourceStatus.markConditionTrue(ConditionTypeConstants.AUGMENTATION,
                        ConditionReasonConstants.DEPLOYMENT_PROGRESSING);
            }
        }
    }

}
