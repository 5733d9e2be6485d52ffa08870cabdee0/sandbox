package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.ServiceMonitorComparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.services.BridgeExecutorServiceMonitorService;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class BridgeExecutorServiceMonitorReconciler {

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    BridgeExecutorServiceMonitorService bridgeExecutorServiceMonitorService;

    public void reconcile(BridgeExecutor bridgeExecutor){

        List<ServiceMonitor> requestResource = createRequiredResources(bridgeExecutor);

        List<ServiceMonitor> deployedResources = fetchDeployedResources(bridgeExecutor);

        processDelta(requestResource, deployedResources);

        /*if (serviceMonitor.isEmpty()) {
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
        }*/
    }

    private List<ServiceMonitor> createRequiredResources(BridgeExecutor bridgeExecutor) {
        ServiceMonitor requestedServiceMonitor = bridgeExecutorServiceMonitorService.createBridgeExecutorServiceMonitorService(bridgeExecutor);
        return Collections.singletonList(requestedServiceMonitor);
    }

    private List<ServiceMonitor> fetchDeployedResources(BridgeExecutor bridgeExecutor) {
        ServiceMonitor deployedServiceMonitor = bridgeExecutorServiceMonitorService.createBridgeExecutorServiceMonitorService(bridgeExecutor);
        return deployedServiceMonitor == null ? Collections.EMPTY_LIST : Collections.singletonList(deployedServiceMonitor);
    }

    private void processDelta(List<ServiceMonitor> requestedResources, List<ServiceMonitor> deployedResources) {
        Comparator<ServiceMonitor> serviceMonitorComparator = new ServiceMonitorComparator();
        deltaProcessorService.processDelta(ServiceMonitor.class, serviceMonitorComparator, requestedResources, deployedResources);
    }
}
