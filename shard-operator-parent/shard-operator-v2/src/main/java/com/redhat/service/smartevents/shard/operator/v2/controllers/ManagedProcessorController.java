package com.redhat.service.smartevents.shard.operator.v2.controllers;

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

import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorHelper;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorInstance;
import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.shard.operator.core.metrics.OperatorMetricsService;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionTypeConstants;
import com.redhat.service.smartevents.shard.operator.core.utils.EventSourceFactory;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v2.ManagedProcessorService;
import com.redhat.service.smartevents.shard.operator.v2.resources.CamelIntegration;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessorStatus;

import io.fabric8.kubernetes.client.KubernetesClient;
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
@ControllerConfiguration(labelSelector = LabelsBuilder.V2_RECONCILER_LABEL_SELECTOR)
public class ManagedProcessorController implements Reconciler<ManagedProcessor>,
        EventSourceInitializer<ManagedProcessor>, ErrorStatusHandler<ManagedProcessor> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedProcessorController.class);

    @ConfigProperty(name = "event-bridge.executor.deployment.timeout-seconds")
    int executorTimeoutSeconds;

    @ConfigProperty(name = "event-bridge.executor.poll-interval.milliseconds")
    int executorPollIntervalMilliseconds;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ManagedProcessorService managedProcessorService;

    @Inject
    BridgeErrorHelper bridgeErrorHelper;

    @Inject
    OperatorMetricsService metricsService;

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<ManagedProcessor> eventSourceContext) {

        List<EventSource> eventSources = new ArrayList<>();
        eventSources.add(EventSourceFactory.buildResourceInformer(kubernetesClient, LabelsBuilder.V2_OPERATOR_NAME,
                ManagedProcessor.COMPONENT_NAME,
                CamelIntegration.class));

        return eventSources;
    }

    @Override
    public UpdateControl<ManagedProcessor> reconcile(ManagedProcessor managedProcessor, Context context) {
        String managedProcessorName = managedProcessor.getMetadata().getName();

        LOGGER.info("Create or update ManagedProcessor: '{}' in namespace '{}'",
                managedProcessorName,
                managedProcessor.getMetadata().getNamespace());

        ManagedProcessorStatus status = managedProcessor.getStatus();

        if (!status.isReady() && isTimedOut(status)) {
            // notifyManagerOfFailure
            status.markConditionFalse(ConditionTypeConstants.READY);
            return UpdateControl.updateStatus(managedProcessor);
        }

        String integrationName = String.format("integration-%s", managedProcessorName);
        CamelIntegration camelIntegration = managedProcessorService.fetchOrCreateCamelIntegration(managedProcessor, integrationName);

        if (camelIntegration == null) {
            LOGGER.info("CamelIntegration for the ManagedProcessor '{}' has not been created yet.",
                    managedProcessorName);
            if (!status.isConditionTypeFalse(ConditionTypeConstants.READY)) {
                status.markConditionFalse(ConditionTypeConstants.READY);
            }
            return UpdateControl.updateStatus(managedProcessor).rescheduleAfter(executorPollIntervalMilliseconds);
        } else if (!status.isConditionTypeTrue(ManagedProcessorStatus.CAMEL_INTEGRATION_AVAILABLE)) {
            status.markConditionTrue(ManagedProcessorStatus.CAMEL_INTEGRATION_AVAILABLE);
        }

        LOGGER.info("Managed Processor: '{}' in namespace '{}' is ready",
                managedProcessorName,
                managedProcessor.getMetadata().getNamespace());

        // Only issue a Status Update once.
        // This is a work-around for non-deterministic Unit Tests.
        // See https://issues.redhat.com/browse/MGDOBR-1002
        if (!managedProcessor.getStatus().isReady()) {
            metricsService.onOperationComplete(managedProcessor, MetricsOperation.CONTROLLER_RESOURCE_PROVISION);
            status.markConditionTrue(ConditionTypeConstants.READY);
            // Notify Manager is Ready
            return UpdateControl.updateStatus(managedProcessor);
        }

        return UpdateControl.noUpdate();
    }

    private boolean isTimedOut(ManagedProcessorStatus status) {
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
    public DeleteControl cleanup(ManagedProcessor ManagedProcessor, Context context) {
        LOGGER.info("Deleted ManagedProcessor: '{}' in namespace '{}'", ManagedProcessor.getMetadata().getName(), ManagedProcessor.getMetadata().getNamespace());

        // Linked resources are automatically deleted

        metricsService.onOperationComplete(ManagedProcessor, MetricsOperation.CONTROLLER_RESOURCE_DELETE);
        // notify manager it's been deleted

        return DeleteControl.defaultDelete();
    }

    @Override
    public Optional<ManagedProcessor> updateErrorStatus(ManagedProcessor ManagedProcessor, RetryInfo retryInfo, RuntimeException e) {
        if (retryInfo.isLastAttempt()) {
            BridgeErrorInstance bei = bridgeErrorHelper.getBridgeErrorInstance(e);
            ManagedProcessor.getStatus().setStatusFromBridgeError(bei);
            // notify manager it's failed
        }
        return Optional.of(ManagedProcessor);
    }

}
