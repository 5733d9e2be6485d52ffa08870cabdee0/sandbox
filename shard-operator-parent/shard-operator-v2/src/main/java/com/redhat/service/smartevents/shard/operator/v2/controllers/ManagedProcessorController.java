package com.redhat.service.smartevents.shard.operator.v2.controllers;

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
import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.shard.operator.core.metrics.OperatorMetricsService;
import com.redhat.service.smartevents.shard.operator.core.networking.NetworkingService;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.core.utils.EventSourceFactory;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v2.ManagedProcessorService;
import com.redhat.service.smartevents.shard.operator.v2.resources.CamelIntegration;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessorStatus;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

@ApplicationScoped
@ControllerConfiguration(name = ManagedProcessorController.NAME, labelSelector = LabelsBuilder.V2_RECONCILER_LABEL_SELECTOR)
public class ManagedProcessorController implements Reconciler<ManagedProcessor>,
        EventSourceInitializer<ManagedProcessor>, ErrorStatusHandler<ManagedProcessor> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedProcessorController.class);
    public static final String NAME = "managedprocessorcontroller";

    @ConfigProperty(name = "event-bridge.executor.deployment.timeout-seconds")
    int executorTimeoutSeconds;

    @ConfigProperty(name = "event-bridge.executor.poll-interval.milliseconds")
    int executorPollIntervalMilliseconds;

    @Inject
    ManagedProcessorService managedProcessorService;

    @Inject
    NetworkingService networkingService;

    @Inject
    OperatorMetricsService metricsService;

    @V2
    @Inject
    BridgeErrorHelper bridgeErrorHelper;

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<ManagedProcessor> eventSourceContext) {
        return EventSourceInitializer.nameEventSources(
                EventSourceFactory.buildInformerFromOwnerReference(eventSourceContext, LabelsBuilder.V2_OPERATOR_NAME, ManagedProcessor.COMPONENT_NAME, Secret.class),
                EventSourceFactory.buildInformerFromOwnerReference(eventSourceContext, LabelsBuilder.V2_OPERATOR_NAME, ManagedProcessor.COMPONENT_NAME, ConfigMap.class),
                EventSourceFactory.buildInformerFromPrimaryResource(eventSourceContext, LabelsBuilder.V2_OPERATOR_NAME, ManagedProcessor.COMPONENT_NAME, CamelIntegration.class),
                networkingService.buildInformerEventSource(eventSourceContext, LabelsBuilder.V2_OPERATOR_NAME, ManagedProcessor.COMPONENT_NAME));
    }

    @Override
    public UpdateControl<ManagedProcessor> reconcile(ManagedProcessor managedProcessor, Context context) {
        String managedProcessorName = managedProcessor.getMetadata().getName();
        String managedProcessorNamespace = managedProcessor.getMetadata().getNamespace();

        LOGGER.info("Reconciling ManagedProcessor: '{}' in namespace '{}'",
                managedProcessorName,
                managedProcessorNamespace);

        ManagedProcessorStatus processorStatus = managedProcessor.getStatus();

        if (!processorStatus.isReadyV2() && isTimedOut(processorStatus)) {
            // The only resource that can be in timeout state is Camel so let's use this to invalidate the ManagedProcessor
            processorStatus.markConditionFalse(ManagedProcessorStatus.CAMEL_INTEGRATION_AVAILABLE);
            return UpdateControl.updateStatus(managedProcessor);
        }

        CamelIntegration camelIntegration = managedProcessorService.fetchOrCreateCamelIntegration(managedProcessor);

        if (!camelIntegration.isReady()) {
            LOGGER.info("CamelIntegration for the ManagedProcessor '{}' in namespace '{}' is not ready",
                    managedProcessorName,
                    managedProcessorNamespace);
            processorStatus.markConditionFalse(ManagedProcessorStatus.CAMEL_INTEGRATION_AVAILABLE);

            return UpdateControl.updateStatus(managedProcessor).rescheduleAfter(executorPollIntervalMilliseconds);
        } else if (!processorStatus.isConditionTypeTrue(ManagedProcessorStatus.CAMEL_INTEGRATION_AVAILABLE)) {
            LOGGER.info("CamelIntegration for the ManagedProcessor '{}' in namespace '{}' is ready",
                    managedProcessorName,
                    managedProcessorNamespace);
            processorStatus.markConditionTrue(ManagedProcessorStatus.CAMEL_INTEGRATION_AVAILABLE);

            // End of provisioning - if more resources will be created in the future the reconcile loop should move forward

            LOGGER.info("Managed Processor: '{}' in namespace '{}' is ready",
                    managedProcessorName,
                    managedProcessorNamespace);

            metricsService.onOperationComplete(managedProcessor, MetricsOperation.CONTROLLER_RESOURCE_PROVISION);
            return UpdateControl.updateStatus(managedProcessor);
        }

        LOGGER.info("Managed Processor: '{}' in namespace '{}' is already provisioned, nothing to do",
                managedProcessorName,
                managedProcessorNamespace);

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
    public ErrorStatusUpdateControl<ManagedProcessor> updateErrorStatus(ManagedProcessor processor, Context<ManagedProcessor> context, Exception e) {
        if (context.getRetryInfo().isPresent() && context.getRetryInfo().get().isLastAttempt()) {
            BridgeErrorInstance bei = bridgeErrorHelper.getBridgeErrorInstance(e);
            processor.getStatus().setStatusFromBridgeError(bei);
            return ErrorStatusUpdateControl.updateStatus(processor);
        }
        return ErrorStatusUpdateControl.noStatusUpdate();
    }
}
