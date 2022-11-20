package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.monitoring.ServiceMonitorClient;
import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BridgeExecutorServiceMonitorServiceImpl implements BridgeExecutorServiceMonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeExecutorServiceMonitorServiceImpl.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TemplateProvider templateProvider;

    @Override
    public ServiceMonitor createBridgeExecutorServiceMonitorService(BridgeExecutor bridgeExecutor) {
        if (!ServiceMonitorClient.isServiceMonitorAvailable(kubernetesClient)) {
            LOGGER.warn("Prometheus Operator is not available in this cluster");
            return null;
        }
        ServiceMonitor expected = templateProvider.loadServiceMonitorTemplate(bridgeExecutor, TemplateImportConfig.withDefaults());
        ensureLabels(expected);
        return expected;
    }

    @Override
    public ServiceMonitor fetchBridgeExecutorServiceMonitorService(BridgeExecutor bridgeExecutor) {
        return kubernetesClient.resources(ServiceMonitor.class).inNamespace(bridgeExecutor.getMetadata().getNamespace())
                .withName(bridgeExecutor.getMetadata().getName())
                .get();
    }

    private void ensureLabels(final ServiceMonitor serviceMonitor) {
        if (serviceMonitor.getSpec().getSelector() == null) {
            serviceMonitor.getSpec().setSelector(new LabelSelector());
        }
        serviceMonitor.getSpec().getSelector().setMatchLabels(new LabelsBuilder()
                .withAppInstance(serviceMonitor.getMetadata().getName())
                .build());
        serviceMonitor.getMetadata().setLabels(new LabelsBuilder()
                .withAppInstance(serviceMonitor.getMetadata().getName())
                .withComponent(BridgeExecutor.COMPONENT_NAME)
                .buildWithDefaults());
    }
}
