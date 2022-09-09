package com.redhat.service.smartevents.shard.operator.controllers;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.exceptions.BridgeErrorHelper;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorInstance;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.PrometheusNotInstalledException;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.ProvisioningReplicaFailureException;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.ProvisioningTimeOutException;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.shard.operator.BridgeExecutorService;
import com.redhat.service.smartevents.shard.operator.ManagerClient;
import com.redhat.service.smartevents.shard.operator.monitoring.ServiceMonitorService;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutorStatus;
import com.redhat.service.smartevents.shard.operator.resources.Condition;
import com.redhat.service.smartevents.shard.operator.resources.ConditionReasonConstants;
import com.redhat.service.smartevents.shard.operator.resources.ConditionStatus;
import com.redhat.service.smartevents.shard.operator.resources.ConditionTypeConstants;
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

import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.FAILED;

@ApplicationScoped
@ControllerConfiguration(labelSelector = LabelsBuilder.RECONCILER_LABEL_SELECTOR)
public class BridgeExecutorController implements Reconciler<BridgeExecutor>,
        EventSourceInitializer<BridgeExecutor>, ErrorStatusHandler<BridgeExecutor> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeExecutorController.class);

    @ConfigProperty(name = "event-bridge.executor.deployment.timeout-seconds")
    int executorTimeoutSeconds;

    @ConfigProperty(name = "event-bridge.executor.poll-interval.seconds")
    int executorPollIntervalSeconds;

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
        LOGGER.info("Create or update BridgeProcessor: '{}' in namespace '{}'",
                bridgeExecutor.getMetadata().getName(),
                bridgeExecutor.getMetadata().getNamespace());

        // Set a "start" time on the creation
        BridgeExecutorStatus status = bridgeExecutor.getStatus();
        if (status.getConditionByType(ConditionTypeConstants.PROGRESSING).isPresent()) {
            if (status.getConditionByType(ConditionTypeConstants.PROGRESSING).get().getStatus() == ConditionStatus.Unknown) {
                status.markConditionTrue(ConditionTypeConstants.PROGRESSING, ConditionReasonConstants.DEPLOYMENT_INITIATED);
                return UpdateControl.updateStatus(bridgeExecutor).rescheduleAfter(executorPollIntervalSeconds);
            }
        }

        if (isTimedOut(bridgeExecutor)) {
            notifyManagerOfFailure(bridgeExecutor,
                    new ProvisioningTimeOutException(String.format(ProvisioningTimeOutException.TIMEOUT_FAILURE_MESSAGE,
                            bridgeExecutor.getClass().getSimpleName(),
                            bridgeExecutor.getSpec().getId())));
            status.markConditionFalse(ConditionTypeConstants.READY);
            status.markConditionFalse(ConditionTypeConstants.AUGMENTATION);
            status.markConditionFalse(ConditionTypeConstants.PROGRESSING);
            return UpdateControl.updateStatus(bridgeExecutor);
        }

        Secret secret = bridgeExecutorService.fetchBridgeExecutorSecret(bridgeExecutor);

        if (secret == null) {
            LOGGER.info("Secrets for the BridgeProcessor '{}' have been not created yet.", bridgeExecutor.getMetadata().getName());
            bridgeExecutor.getStatus().markConditionFalse(ConditionTypeConstants.READY);
            bridgeExecutor.getStatus().markConditionTrue(ConditionTypeConstants.AUGMENTATION, ConditionReasonConstants.SECRETS_NOT_FOUND);
            return UpdateControl.updateStatus(bridgeExecutor).rescheduleAfter(executorPollIntervalSeconds);
        } else {
            // Update "Last Transition Time", effectively resetting time-out.
            status.getConditionByType(ConditionTypeConstants.AUGMENTATION).ifPresent(c -> {
                if (Objects.equals(c.getReason(), ConditionReasonConstants.SECRETS_NOT_FOUND)) {
                    status.markConditionTrue(ConditionTypeConstants.PROGRESSING, ConditionReasonConstants.SECRETS_FOUND);
                }
            });
        }

        // Check if the image of the executor has to be updated
        String image = bridgeExecutorService.getExecutorImage();
        if (!image.equals(bridgeExecutor.getSpec().getImage())) {
            bridgeExecutor.getStatus().markConditionFalse(ConditionTypeConstants.READY);
            bridgeExecutor.getStatus().markConditionTrue(ConditionTypeConstants.AUGMENTATION, ConditionReasonConstants.IMAGE_NOT_CORRECT);
            bridgeExecutor.getSpec().setImage(image);
            return UpdateControl.updateResourceAndStatus(bridgeExecutor).rescheduleAfter(executorPollIntervalSeconds);
        } else {
            // Update "Last Transition Time", effectively resetting time-out.
            status.getConditionByType(ConditionTypeConstants.AUGMENTATION).ifPresent(c -> {
                if (Objects.equals(c.getReason(), ConditionReasonConstants.IMAGE_NOT_CORRECT)) {
                    status.markConditionTrue(ConditionTypeConstants.PROGRESSING, ConditionReasonConstants.IMAGE_CORRECT);
                }
            });
        }

        Deployment deployment = bridgeExecutorService.fetchOrCreateBridgeExecutorDeployment(bridgeExecutor, secret);
        if (!Readiness.isDeploymentReady(deployment)) {
            LOGGER.info("Executor deployment BridgeProcessor: '{}' in namespace '{}' is NOT ready",
                    bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());

            bridgeExecutor.getStatus().setStatusFromDeployment(deployment);

            if (DeploymentStatusUtils.isTimeoutFailure(deployment)) {
                notifyManagerOfFailure(bridgeExecutor,
                        new ProvisioningTimeOutException(DeploymentStatusUtils.getReasonAndMessageForTimeoutFailure(deployment)));
            } else if (DeploymentStatusUtils.isStatusReplicaFailure(deployment)) {
                notifyManagerOfFailure(bridgeExecutor,
                        new ProvisioningReplicaFailureException(DeploymentStatusUtils.getReasonAndMessageForReplicaFailure(deployment)));
            }

            return UpdateControl.updateStatus(bridgeExecutor).rescheduleAfter(executorPollIntervalSeconds);
        } else {
            // Update "Last Transition Time", effectively resetting time-out.
            status.getConditionByType(ConditionTypeConstants.AUGMENTATION).ifPresent(c -> {
                if (Objects.equals(c.getReason(), ConditionReasonConstants.DEPLOYMENT_PROGRESSING)
                        || Objects.equals(c.getReason(), ConditionReasonConstants.DEPLOYMENT_FAILED)) {
                    status.markConditionTrue(ConditionTypeConstants.PROGRESSING, ConditionReasonConstants.DEPLOYMENT_AVAILABLE);
                }
            });
        }
        LOGGER.info("Executor deployment BridgeProcessor: '{}' in namespace '{}' is ready", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());

        // Create Service
        Service service = bridgeExecutorService.fetchOrCreateBridgeExecutorService(bridgeExecutor, deployment);
        if (service.getStatus() == null) {
            LOGGER.info("Executor service BridgeProcessor: '{}' in namespace '{}' is NOT ready", bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());
            bridgeExecutor.getStatus().markConditionFalse(ConditionTypeConstants.READY);
            bridgeExecutor.getStatus().markConditionTrue(ConditionTypeConstants.AUGMENTATION, ConditionReasonConstants.SERVICE_NOT_READY);
            return UpdateControl.updateStatus(bridgeExecutor).rescheduleAfter(executorPollIntervalSeconds);
        } else {
            // Update "Last Transition Time", effectively resetting time-out.
            status.getConditionByType(ConditionTypeConstants.AUGMENTATION).ifPresent(c -> {
                if (Objects.equals(c.getReason(), ConditionReasonConstants.SERVICE_NOT_READY)) {
                    status.markConditionTrue(ConditionTypeConstants.PROGRESSING, ConditionReasonConstants.SERVICE_READY);
                }
            });
        }

        Optional<ServiceMonitor> serviceMonitor = monitorService.fetchOrCreateServiceMonitor(bridgeExecutor, service, BridgeExecutor.COMPONENT_NAME);
        if (serviceMonitor.isPresent()) {
            // this is an optional resource
            LOGGER.info("Executor service monitor resource BridgeExecutor: '{}' in namespace '{}' is ready", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());
        } else {
            LOGGER.warn("Executor service monitor resource BridgeExecutor: '{}' in namespace '{}' is failed to deploy, Prometheus not installed.", bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());
            PrometheusNotInstalledException prometheusNotInstalledException = new PrometheusNotInstalledException(ConditionReasonConstants.PROMETHEUS_UNAVAILABLE);
            BridgeErrorInstance bei = bridgeErrorHelper.getBridgeErrorInstance(prometheusNotInstalledException);
            bridgeExecutor.getStatus().setStatusFromBridgeError(bei);
            notifyManagerOfFailure(bridgeExecutor, bei);
            return UpdateControl.updateStatus(bridgeExecutor).rescheduleAfter(executorPollIntervalSeconds);
        }

        LOGGER.info("Executor service BridgeProcessor: '{}' in namespace '{}' is ready", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());

        // Only issue a Status Update once.
        // This is a work-around for non-deterministic Unit Tests.
        // See https://issues.redhat.com/browse/MGDOBR-1002
        if (!bridgeExecutor.getStatus().isReady()) {
            bridgeExecutor.getStatus().markConditionTrue(ConditionTypeConstants.READY);
            bridgeExecutor.getStatus().markConditionFalse(ConditionTypeConstants.AUGMENTATION);
            notifyManager(bridgeExecutor, ManagedResourceStatus.READY);
            return UpdateControl.updateStatus(bridgeExecutor);
        }

        return UpdateControl.noUpdate();
    }

    private boolean isTimedOut(BridgeExecutor bridgeExecutor) {
        Optional<Condition> progress = bridgeExecutor.getStatus().getConditionByType(ConditionTypeConstants.PROGRESSING);
        if (progress.isPresent()) {
            Date lastTransitionTime = progress.get().getLastTransitionTime();
            ZonedDateTime zonedLastTransition = ZonedDateTime.ofInstant(lastTransitionTime.toInstant(), ZoneOffset.UTC);
            ZonedDateTime zonedNow = ZonedDateTime.now(ZoneOffset.UTC);
            return zonedNow.minus(Duration.of(executorTimeoutSeconds, ChronoUnit.SECONDS)).isAfter(zonedLastTransition);
        }
        return false;
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
            BridgeErrorInstance bei = bridgeErrorHelper.getBridgeErrorInstance(e);
            bridgeExecutor.getStatus().setStatusFromBridgeError(bei);
            notifyManagerOfFailure(bridgeExecutor, bei);
        }
        return Optional.of(bridgeExecutor);
    }

    private void notifyManager(BridgeExecutor bridgeExecutor, ManagedResourceStatus status) {
        ProcessorManagedResourceStatusUpdateDTO updateDTO =
                new ProcessorManagedResourceStatusUpdateDTO(bridgeExecutor.getSpec().getId(), bridgeExecutor.getSpec().getCustomerId(), bridgeExecutor.getSpec().getBridgeId(), status);
        managerClient.notifyProcessorStatusChange(updateDTO)
                .subscribe().with(
                        success -> LOGGER.info("Updating Processor with id '{}' done", updateDTO.getId()),
                        failure -> LOGGER.error("Updating Processor with id '{}' FAILED", updateDTO.getId(), failure));
    }

    private void notifyManagerOfFailure(BridgeExecutor bridgeExecutor, Exception e) {
        BridgeErrorInstance bei = bridgeErrorHelper.getBridgeErrorInstance(e);
        notifyManagerOfFailure(bridgeExecutor, bei);
    }

    private void notifyManagerOfFailure(BridgeExecutor bridgeExecutor, BridgeErrorInstance bei) {
        LOGGER.error("BridgeExecutor: '{}' in namespace '{}' has failed with reason: '{}'",
                bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace(), bei.getReason());

        String id = bridgeExecutor.getSpec().getId();
        String customerId = bridgeExecutor.getSpec().getCustomerId();
        String bridgeId = bridgeExecutor.getSpec().getBridgeId();
        ProcessorManagedResourceStatusUpdateDTO dto = new ProcessorManagedResourceStatusUpdateDTO(id,
                customerId,
                bridgeId,
                FAILED,
                bei);

        managerClient.notifyProcessorStatusChange(dto)
                .subscribe().with(
                        success -> LOGGER.info("Updating Processor with id '{}' done", dto.getId()),
                        failure -> LOGGER.error("Updating Processor with id '{}' FAILED", dto.getId(), failure));
    }

}
