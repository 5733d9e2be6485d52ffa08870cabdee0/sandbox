package com.redhat.service.smartevents.shard.operator.controllers;

import com.redhat.service.smartevents.shard.operator.ManagerClient;
import com.redhat.service.smartevents.shard.operator.reconcilers.BridgeExecutorDeploymentReconciler;
import com.redhat.service.smartevents.shard.operator.reconcilers.BridgeExecutorSecretReconciler;
import com.redhat.service.smartevents.shard.operator.reconcilers.BridgeExecutorServiceMonitorReconciler;
import com.redhat.service.smartevents.shard.operator.reconcilers.BridgeExecutorServiceReconciler;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.utils.EventSourceFactory;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    BridgeExecutorSecretReconciler bridgeExecutorSecretReconciler;

    @Inject
    BridgeExecutorDeploymentReconciler bridgeExecutorDeploymentReconciler;

    @Inject
    BridgeExecutorServiceReconciler bridgeExecutorServiceReconciler;

    @Inject
    BridgeExecutorServiceMonitorReconciler bridgeExecutorServiceMonitorReconciler;

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
        try {
        bridgeExecutorSecretReconciler.reconcile(bridgeExecutor);

        bridgeExecutorDeploymentReconciler.reconcile(bridgeExecutor);

        bridgeExecutorServiceReconciler.reconcile(bridgeExecutor);

        bridgeExecutorServiceMonitorReconciler.reconcile(bridgeExecutor);

        LOGGER.info("Executor service BridgeProcessor: '{}' in namespace '{}' is ready",
                bridgeExecutor.getMetadata().getName(),
                bridgeExecutor.getMetadata().getNamespace());

        } catch (RuntimeException e) {
            managerClient.notifyProcessorStatusChange(bridgeExecutor.getSpec().getId(), bridgeExecutor.getStatus().getConditions());
            return UpdateControl.updateStatus(bridgeExecutor);
        }

        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl cleanup(BridgeExecutor bridgeExecutor, Context context) {
        /*LOGGER.info("Deleted BridgeProcessor: '{}' in namespace '{}'", bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());

        // Linked resources are automatically deleted
        metricsService.onOperationComplete(bridgeExecutor, MetricsOperation.CONTROLLER_RESOURCE_DELETE);
        notifyManager(bridgeExecutor, ManagedResourceStatus.DELETED);*/
        return DeleteControl.defaultDelete();
    }

    @Override
    public Optional<BridgeExecutor> updateErrorStatus(BridgeExecutor bridgeExecutor, RetryInfo retryInfo, RuntimeException e) {
        /*if (retryInfo.isLastAttempt()) {
            BridgeErrorInstance bei = bridgeErrorHelper.getBridgeErrorInstance(e);
            bridgeExecutor.getStatus().setStatusFromBridgeError(bei);
            notifyManagerOfFailure(bridgeExecutor, bei);
        }*/
        return Optional.of(bridgeExecutor);
    }
}
