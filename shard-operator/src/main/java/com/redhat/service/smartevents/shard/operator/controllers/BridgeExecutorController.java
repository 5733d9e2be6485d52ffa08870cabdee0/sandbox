package com.redhat.service.smartevents.shard.operator.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.exceptions.BridgeError;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.PrometheusNotInstalledException;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.shard.operator.BridgeExecutorService;
import com.redhat.service.smartevents.shard.operator.ManagerClient;
import com.redhat.service.smartevents.shard.operator.camel.CamelService;
import com.redhat.service.smartevents.shard.operator.monitoring.ServiceMonitorService;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.ConditionReasonConstants;
import com.redhat.service.smartevents.shard.operator.resources.ConditionTypeConstants;
import com.redhat.service.smartevents.shard.operator.resources.camel.CamelIntegration;
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
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

@ApplicationScoped
@ControllerConfiguration(labelSelector = LabelsBuilder.RECONCILER_LABEL_SELECTOR)
public class BridgeExecutorController implements Reconciler<BridgeExecutor>,
        EventSourceInitializer<BridgeExecutor> {

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
    CamelService camelService;

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

        ProcessorDTO processorDTO = bridgeExecutor.toDTO();
        ProcessorDefinition definition = processorDTO.getDefinition();
        if (definition != null && definition.getProcessing() == null) {
            LOGGER.info("---- No camel processing found. Provisioning executor.");

            // Check if the image of the executor has to be updated
            String image = bridgeExecutorService.getExecutorImage();
            if (!image.equals(bridgeExecutor.getSpec().getImage())) {
                bridgeExecutor.getSpec().setImage(image);
                return UpdateControl.updateResource(bridgeExecutor);
            }

            Deployment deployment = bridgeExecutorService.fetchOrCreateBridgeExecutorDeployment(bridgeExecutor, secret);
            if (!Readiness.isDeploymentReady(deployment)) {
                LOGGER.debug("Executor deployment BridgeProcessor: '{}' in namespace '{}' is NOT ready", bridgeExecutor.getMetadata().getName(),
                        bridgeExecutor.getMetadata().getNamespace());

                bridgeExecutor.getStatus().setConditionsFromDeployment(deployment);

                if (DeploymentStatusUtils.isTimeoutFailure(deployment)) {
                    notifyDeploymentFailure(bridgeExecutor, DeploymentStatusUtils.getReasonAndMessageForTimeoutFailure(deployment));
                } else if (DeploymentStatusUtils.isStatusReplicaFailure(deployment)) {
                    notifyDeploymentFailure(bridgeExecutor, DeploymentStatusUtils.getReasonAndMessageForReplicaFailure(deployment));
                }

                return UpdateControl.updateStatus(bridgeExecutor);
            }

            LOGGER.debug("Executor deployment BridgeProcessor: '{}' in namespace '{}' is ready", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());

            // Create Service
            Service service = bridgeExecutorService.fetchOrCreateBridgeExecutorService(bridgeExecutor, deployment);
            if (service.getStatus() == null) {
                LOGGER.debug("Executor service BridgeProcessor: '{}' in namespace '{}' is NOT ready", bridgeExecutor.getMetadata().getName(),
                        bridgeExecutor.getMetadata().getNamespace());
                bridgeExecutor.getStatus().markConditionFalse(ConditionTypeConstants.READY);
                bridgeExecutor.getStatus().markConditionTrue(ConditionTypeConstants.AUGMENTATION, ConditionReasonConstants.SERVICE_NOT_READY);
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
                bridgeExecutor.getStatus().markConditionFalse(ConditionTypeConstants.READY,
                        ConditionReasonConstants.PROMETHEUS_UNAVAILABLE,
                        prometheusNotAvailableError.getReason(),
                        prometheusNotAvailableError.getCode());
                notifyManager(bridgeExecutor, ManagedResourceStatus.FAILED);
                return UpdateControl.updateStatus(bridgeExecutor);
            }

        } else {
            LOGGER.info("---- Camel processing found. Not provisioning executor provisioning Camel resource instead");

            Optional<CamelIntegration> camelIntegration = camelService.fetchOrCreateCamelIntegration(bridgeExecutor, secret);
            if (camelIntegration.isPresent()) {
                // this is an optional resource
                LOGGER.debug("CameIntegration resource {} for BridgeExecutor: '{}' in namespace '{}' is ready", camelIntegration.get(),
                        bridgeExecutor.getMetadata().getName(),
                        bridgeExecutor.getMetadata().getNamespace());
            }
        }

        LOGGER.debug("Executor service BridgeProcessor: '{}' in namespace '{}' is ready", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());

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

    private void notifyDeploymentFailure(BridgeExecutor bridgeExecutor, String failureReason) {
        LOGGER.warn("Processor deployment BridgeExecutor: '{}' in namespace '{}' has failed with reason: '{}'",
                bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace(), failureReason);
        notifyManager(bridgeExecutor, ManagedResourceStatus.FAILED);
    }

    private void notifyManager(BridgeExecutor bridgeExecutor, ManagedResourceStatus status) {
        ProcessorDTO dto = bridgeExecutor.toDTO();
        dto.setStatus(status);

        managerClient.notifyProcessorStatusChange(dto)
                .subscribe().with(
                        success -> LOGGER.info("Updating Processor with id '{}' done", dto.getId()),
                        failure -> LOGGER.error("Updating Processor with id '{}' FAILED", dto.getId()));
    }
}
