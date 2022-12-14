package com.redhat.service.smartevents.shard.operator.v1.controllers;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorHelper;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorInstance;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.PrometheusNotInstalledException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.ProvisioningReplicaFailureException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.ProvisioningTimeOutException;
import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.v1.api.dto.ProcessorManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.shard.operator.core.metrics.OperatorMetricsService;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionReasonConstants;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionTypeConstants;
import com.redhat.service.smartevents.shard.operator.core.utils.DeploymentStatusUtils;
import com.redhat.service.smartevents.shard.operator.core.utils.EventSourceFactory;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v1.BridgeExecutorService;
import com.redhat.service.smartevents.shard.operator.v1.ManagerClient;
import com.redhat.service.smartevents.shard.operator.v1.monitoring.ServiceMonitorService;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeExecutorStatus;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeIngress;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.FAILED;

@ApplicationScoped
@ControllerConfiguration(labelSelector = LabelsBuilder.V1_RECONCILER_LABEL_SELECTOR)
public class BridgeExecutorController implements Reconciler<BridgeExecutor>,
        EventSourceInitializer<BridgeExecutor>,
        ErrorStatusHandler<BridgeExecutor>,
        Cleaner<BridgeExecutor> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeExecutorController.class);

    @ConfigProperty(name = "event-bridge.executor.deployment.timeout-seconds")
    int executorTimeoutSeconds;

    @ConfigProperty(name = "event-bridge.executor.poll-interval.milliseconds")
    int executorPollIntervalMilliseconds;

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

    @Inject
    OperatorMetricsService metricsService;

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<BridgeExecutor> eventSourceContext) {
        return EventSourceInitializer.nameEventSources(EventSourceFactory.buildSecretsInformer(eventSourceContext, LabelsBuilder.V1_OPERATOR_NAME, BridgeIngress.COMPONENT_NAME),
                EventSourceFactory.buildDeploymentsInformer(eventSourceContext, LabelsBuilder.V1_OPERATOR_NAME, BridgeIngress.COMPONENT_NAME),
                EventSourceFactory.buildServicesInformer(eventSourceContext, LabelsBuilder.V1_OPERATOR_NAME, BridgeIngress.COMPONENT_NAME),
                EventSourceFactory.buildServicesMonitorInformer(eventSourceContext, LabelsBuilder.V1_OPERATOR_NAME, BridgeIngress.COMPONENT_NAME));
    }

    @Override
    public UpdateControl<BridgeExecutor> reconcile(BridgeExecutor bridgeExecutor, Context context) {
        LOGGER.info("Create or update BridgeProcessor: '{}' in namespace '{}'",
                bridgeExecutor.getMetadata().getName(),
                bridgeExecutor.getMetadata().getNamespace());

        BridgeExecutorStatus status = bridgeExecutor.getStatus();

        if (!status.isReady() && isTimedOut(status)) {
            notifyManagerOfFailure(bridgeExecutor,
                    new ProvisioningTimeOutException(String.format(ProvisioningTimeOutException.TIMEOUT_FAILURE_MESSAGE,
                            bridgeExecutor.getClass().getSimpleName(),
                            bridgeExecutor.getSpec().getId())));
            status.markConditionFalse(ConditionTypeConstants.READY);
            return UpdateControl.updateStatus(bridgeExecutor);
        }

        Secret secret = bridgeExecutorService.fetchBridgeExecutorSecret(bridgeExecutor);

        if (secret == null) {
            LOGGER.info("Secrets for the BridgeProcessor '{}' have been not created yet.",
                    bridgeExecutor.getMetadata().getName());
            if (!status.isConditionTypeFalse(ConditionTypeConstants.READY)) {
                status.markConditionFalse(ConditionTypeConstants.READY);
            }
            if (!status.isConditionTypeFalse(BridgeExecutorStatus.SECRET_AVAILABLE)) {
                status.markConditionFalse(BridgeExecutorStatus.SECRET_AVAILABLE);
            }
            return UpdateControl.updateStatus(bridgeExecutor).rescheduleAfter(executorPollIntervalMilliseconds);
        } else if (!status.isConditionTypeTrue(BridgeExecutorStatus.SECRET_AVAILABLE)) {
            status.markConditionTrue(BridgeExecutorStatus.SECRET_AVAILABLE);
        }

        // Check if the image of the executor has to be updated
        String image = bridgeExecutorService.getExecutorImage();
        if (!image.equals(bridgeExecutor.getSpec().getImage())) {
            if (!status.isConditionTypeFalse(ConditionTypeConstants.READY)) {
                status.markConditionFalse(ConditionTypeConstants.READY);
            }
            if (!status.isConditionTypeFalse(BridgeExecutorStatus.IMAGE_NAME_CORRECT)) {
                status.markConditionFalse(BridgeExecutorStatus.IMAGE_NAME_CORRECT);
            }
            bridgeExecutor.getSpec().setImage(image);
            return UpdateControl.updateResourceAndStatus(bridgeExecutor).rescheduleAfter(executorPollIntervalMilliseconds);
        } else if (!status.isConditionTypeTrue(BridgeExecutorStatus.IMAGE_NAME_CORRECT)) {
            status.markConditionTrue(BridgeExecutorStatus.IMAGE_NAME_CORRECT);
        }

        Deployment deployment = bridgeExecutorService.fetchOrCreateBridgeExecutorDeployment(bridgeExecutor, secret);
        if (!Readiness.isDeploymentReady(deployment)) {
            LOGGER.info("Executor deployment BridgeProcessor: '{}' in namespace '{}' is NOT ready",
                    bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());

            status.setStatusFromDeployment(deployment);

            if (DeploymentStatusUtils.isTimeoutFailure(deployment)) {
                notifyManagerOfFailure(bridgeExecutor,
                        new ProvisioningTimeOutException(DeploymentStatusUtils.getReasonAndMessageForTimeoutFailure(deployment)));
                // Don't reschedule reconciliation if we're in a FAILED state
                return UpdateControl.updateStatus(bridgeExecutor);
            } else if (DeploymentStatusUtils.isStatusReplicaFailure(deployment)) {
                notifyManagerOfFailure(bridgeExecutor,
                        new ProvisioningReplicaFailureException(DeploymentStatusUtils.getReasonAndMessageForReplicaFailure(deployment)));
                // Don't reschedule reconciliation if we're in a FAILED state
                return UpdateControl.updateStatus(bridgeExecutor);
            } else {
                // State may otherwise be recoverable so reschedule
                return UpdateControl.updateStatus(bridgeExecutor).rescheduleAfter(executorPollIntervalMilliseconds);
            }
        } else {
            LOGGER.info("Executor deployment BridgeProcessor: '{}' in namespace '{}' is ready",
                    bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());
            if (!status.isConditionTypeTrue(BridgeExecutorStatus.DEPLOYMENT_AVAILABLE)) {
                status.markConditionTrue(BridgeExecutorStatus.DEPLOYMENT_AVAILABLE);
            }
        }

        // Create Service
        Service service = bridgeExecutorService.fetchOrCreateBridgeExecutorService(bridgeExecutor, deployment);
        if (service.getStatus() == null) {
            LOGGER.info("Executor service BridgeProcessor: '{}' in namespace '{}' is NOT ready",
                    bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());
            if (!status.isConditionTypeFalse(ConditionTypeConstants.READY)) {
                status.markConditionFalse(ConditionTypeConstants.READY);
            }
            if (!status.isConditionTypeFalse(BridgeExecutorStatus.SERVICE_AVAILABLE)) {
                status.markConditionFalse(BridgeExecutorStatus.SERVICE_AVAILABLE);
            }
            return UpdateControl.updateStatus(bridgeExecutor).rescheduleAfter(executorPollIntervalMilliseconds);
        } else if (!status.isConditionTypeTrue(BridgeExecutorStatus.SERVICE_AVAILABLE)) {
            status.markConditionTrue(BridgeExecutorStatus.SERVICE_AVAILABLE);
        }

        Optional<ServiceMonitor> serviceMonitor = monitorService.fetchOrCreateServiceMonitor(bridgeExecutor, service, BridgeExecutor.COMPONENT_NAME);
        if (serviceMonitor.isEmpty()) {
            LOGGER.warn("Executor service monitor resource BridgeExecutor: '{}' in namespace '{}' is failed to deploy, Prometheus not installed.",
                    bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());
            if (!status.isConditionTypeFalse(BridgeExecutorStatus.SERVICE_MONITOR_AVAILABLE)) {
                status.markConditionFalse(BridgeExecutorStatus.SERVICE_MONITOR_AVAILABLE);
            }
            PrometheusNotInstalledException prometheusNotInstalledException = new PrometheusNotInstalledException(ConditionReasonConstants.PROMETHEUS_UNAVAILABLE);
            BridgeErrorInstance bei = bridgeErrorHelper.getBridgeErrorInstance(prometheusNotInstalledException);
            status.setStatusFromBridgeError(bei);
            notifyManagerOfFailure(bridgeExecutor, bei);

            return UpdateControl.updateStatus(bridgeExecutor);
        } else {
            // this is an optional resource
            LOGGER.info("Executor service monitor resource BridgeExecutor: '{}' in namespace '{}' is ready",
                    bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());
            if (!status.isConditionTypeTrue(BridgeExecutorStatus.SERVICE_MONITOR_AVAILABLE)) {
                status.markConditionTrue(BridgeExecutorStatus.SERVICE_MONITOR_AVAILABLE);
            }
        }

        LOGGER.info("Executor service BridgeProcessor: '{}' in namespace '{}' is ready",
                bridgeExecutor.getMetadata().getName(),
                bridgeExecutor.getMetadata().getNamespace());

        // Only issue a Status Update once.
        // This is a work-around for non-deterministic Unit Tests.
        // See https://issues.redhat.com/browse/MGDOBR-1002
        if (!bridgeExecutor.getStatus().isReady()) {
            metricsService.onOperationComplete(bridgeExecutor, MetricsOperation.CONTROLLER_RESOURCE_PROVISION);
            status.markConditionTrue(ConditionTypeConstants.READY);
            notifyManager(bridgeExecutor, ManagedResourceStatus.READY);
            return UpdateControl.updateStatus(bridgeExecutor);
        }

        return UpdateControl.noUpdate();
    }

    private boolean isTimedOut(BridgeExecutorStatus status) {
        Optional<Date> lastTransitionDate = status.getConditions()
                .stream()
                .filter(c -> Objects.nonNull(c.getLastTransitionTime()))
                .reduce((c1, c2) -> c1.getLastTransitionTime().after(c2.getLastTransitionTime()) ? c1 : c2)
                .map(Condition::getLastTransitionTime);

        if (lastTransitionDate.isEmpty()) {
            return false;
        }

        ZonedDateTime zonedLastTransition = ZonedDateTime.ofInstant(lastTransitionDate.get().toInstant(), ZoneOffset.UTC);
        ZonedDateTime zonedNow = ZonedDateTime.now(ZoneOffset.UTC);
        return zonedNow.minus(Duration.of(executorTimeoutSeconds, ChronoUnit.SECONDS)).isAfter(zonedLastTransition);
    }

    @Override
    public DeleteControl cleanup(BridgeExecutor bridgeExecutor, Context context) {
        LOGGER.info("Deleted BridgeProcessor: '{}' in namespace '{}'", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());

        // Linked resources are automatically deleted

        metricsService.onOperationComplete(bridgeExecutor, MetricsOperation.CONTROLLER_RESOURCE_DELETE);
        notifyManager(bridgeExecutor, ManagedResourceStatus.DELETED);

        return DeleteControl.defaultDelete();
    }

    @Override
    public ErrorStatusUpdateControl<BridgeExecutor> updateErrorStatus(BridgeExecutor bridgeExecutor, Context<BridgeExecutor> context, Exception e) {
        if (context.getRetryInfo().isPresent() && context.getRetryInfo().get().isLastAttempt()) {
            BridgeErrorInstance bei = bridgeErrorHelper.getBridgeErrorInstance(e);
            bridgeExecutor.getStatus().setStatusFromBridgeError(bei);
            notifyManagerOfFailure(bridgeExecutor, bei);
        }
        return ErrorStatusUpdateControl.updateStatus(bridgeExecutor);
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

        metricsService.onOperationFailed(bridgeExecutor, MetricsOperation.CONTROLLER_RESOURCE_PROVISION);

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
