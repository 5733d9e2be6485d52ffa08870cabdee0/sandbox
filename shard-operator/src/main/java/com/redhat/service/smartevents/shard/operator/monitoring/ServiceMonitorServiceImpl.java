package com.redhat.service.smartevents.shard.operator.monitoring;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;

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
    public Optional<ServiceMonitor> fetchOrCreateServiceMonitor(final CustomResource resource, final Service service, final String componentName) {
        if (!ServiceMonitorClient.isServiceMonitorAvailable(kubernetesClient)) {
            LOGGER.warn("Prometheus Operator is not available in this cluster");
            return Optional.empty();
        }

        ServiceMonitor expected = templateProvider.loadServiceMonitorTemplate(resource, TemplateImportConfig.withAll());
        if (expected.getSpec().getSelector() == null) {
            expected.getSpec().setSelector(new LabelSelector());
        }
        this.ensureLabels(expected, service, componentName);

        ServiceMonitor existing = ServiceMonitorClient
                .get(kubernetesClient)
                .inNamespace(resource.getMetadata().getNamespace())
                .withName(service.getMetadata().getName())
                .get();

        if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            return Optional.of(ServiceMonitorClient
                    .get(kubernetesClient)
                    .inNamespace(resource.getMetadata().getNamespace())
                    .withName(service.getMetadata().getName())
                    .createOrReplace(expected));
        }

        return Optional.of(existing);
    }

    private void ensureLabels(final ServiceMonitor serviceMonitor, final Service service, final String component) {
        serviceMonitor.getSpec().getSelector().setMatchLabels(new LabelsBuilder().withAppInstance(service.getMetadata().getName()).build());
        serviceMonitor.getMetadata().setLabels(new LabelsBuilder().withAppInstance(service.getMetadata().getName()).withComponent(component).buildWithDefaults());
    }
}
