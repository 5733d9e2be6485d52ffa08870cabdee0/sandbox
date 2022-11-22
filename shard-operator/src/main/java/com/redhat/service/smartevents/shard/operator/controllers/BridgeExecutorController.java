package com.redhat.service.smartevents.shard.operator.controllers;

import com.redhat.service.smartevents.shard.operator.ManagerClient;
import com.redhat.service.smartevents.shard.operator.reconcilers.BridgeExecutorDeploymentReconciler;
import com.redhat.service.smartevents.shard.operator.reconcilers.BridgeExecutorSecretReconciler;
import com.redhat.service.smartevents.shard.operator.reconcilers.BridgeExecutorServiceMonitorReconciler;
import com.redhat.service.smartevents.shard.operator.reconcilers.BridgeExecutorServiceReconciler;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.services.ReconciliationResultService;
import com.redhat.service.smartevents.shard.operator.utils.EventSourceFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@ControllerConfiguration()
public class BridgeExecutorController implements Reconciler<BridgeExecutor>,
        EventSourceInitializer<BridgeExecutor> {

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

    @Inject
    ReconciliationResultService reconciliationResultService;

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
        }
        finally {
            //managerClient.notifyBridgeStatusChange(bridgeIngress.getSpec().getId(), bridgeIngress.getStatus().getConditions());
        }
        return reconciliationResultService.getReconciliationResult(bridgeExecutor);
    }
}
