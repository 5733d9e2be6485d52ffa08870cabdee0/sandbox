package com.redhat.service.bridge.shard.operator.monitoring;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.shard.operator.providers.TemplateProvider;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

@ApplicationScoped
public class ServiceMonitorServiceImpl implements ServiceMonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceMonitorServiceImpl.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TemplateProvider templateProvider;

    @Override
    public Optional<ServiceMonitor> fetchOrCreateServiceMonitor(final CustomResource resource, final Service service) {
        if (!ServiceMonitorClient.isServiceMonitorAvailable(kubernetesClient)) {
            LOGGER.debug("Prometheus Operator is not available in this cluster");
            return Optional.empty();
        }
        ServiceMonitor serviceMonitor = ServiceMonitorClient
                .get(kubernetesClient)
                .inNamespace(resource.getMetadata().getNamespace())
                .withName(service.getMetadata().getName())
                .get();
        if (serviceMonitor == null) {
            LOGGER.debug("Prometheus ServiceMonitor does not exist. Creating a new instance.");
            serviceMonitor = templateProvider.loadServiceMonitorTemplate(resource);
            if (serviceMonitor.getSpec().getSelector() == null) {
                serviceMonitor.getSpec().setSelector(new LabelSelector());
            }
            serviceMonitor.getSpec().getSelector().setMatchLabels(new LabelsBuilder().withAppInstance(service.getMetadata().getName()).build());
            serviceMonitor = ServiceMonitorClient
                    .get(kubernetesClient)
                    .inNamespace(resource.getMetadata().getNamespace())
                    .withName(service.getMetadata().getName())
                    .create(serviceMonitor);
        }
        return Optional.of(serviceMonitor);
    }
}
