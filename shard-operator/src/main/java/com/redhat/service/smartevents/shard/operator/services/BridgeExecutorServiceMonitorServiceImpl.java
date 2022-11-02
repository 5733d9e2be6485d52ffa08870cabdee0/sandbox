package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.monitoring.ServiceMonitorClient;
import com.redhat.service.smartevents.shard.operator.monitoring.ServiceMonitorServiceImpl;
import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
public class BridgeExecutorServiceMonitorServiceImpl implements BridgeExecutorServiceMonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceMonitorServiceImpl.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TemplateProvider templateProvider;

    @Override
    public ServiceMonitor createBridgeExecutorServiceMonitorService(BridgeExecutor bridgeExecutor) {
        if (!ServiceMonitorClient.isServiceMonitorAvailable(kubernetesClient)) {
            LOGGER.warn("Prometheus Operator is not available in this cluster");
            return Optional.empty();
        }

        ServiceMonitor expected = templateProvider.loadServiceMonitorTemplate(resource, TemplateImportConfig.withDefaults());
        if (expected.getSpec().getSelector() == null) {
            expected.getSpec().setSelector(new LabelSelector());
        }
        this.ensureLabels(expected, service, componentName);


    }

    @Override
    public ServiceMonitor fetchBridgeExecutorServiceMonitorService(BridgeExecutor bridgeExecutor) {
        return kubernetesClient.resources(ServiceMonitor.class).inNamespace(bridgeExecutor.getMetadata().getNamespace())
                .withName(bridgeExecutor.getMetadata().getName())
                .get();
    }

    private void ensureLabels(final ServiceMonitor serviceMonitor, final Service service, final String component) {
        serviceMonitor.getSpec().getSelector().setMatchLabels(new LabelsBuilder().withAppInstance(service.getMetadata().getName()).build());
        serviceMonitor.getMetadata().setLabels(new LabelsBuilder().withAppInstance(service.getMetadata().getName()).withComponent(component).buildWithDefaults());
    }
}
