package com.redhat.service.rhose.shard.operator.monitoring;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

/**
 * Collection of shortcuts to interact with the Prometheus ServiceMonitor Resource
 */
public final class ServiceMonitorClient {

    public static final String SERVICE_MONITOR_CRD_NAME = "servicemonitors.monitoring.coreos.com";

    private ServiceMonitorClient() {
    }

    public static MixedOperation<ServiceMonitor, KubernetesResourceList<ServiceMonitor>, Resource<ServiceMonitor>> get(final KubernetesClient kubernetesClient) {
        return kubernetesClient.resources(ServiceMonitor.class);
    }

    public static boolean isServiceMonitorAvailable(final KubernetesClient kubernetesClient) {
        final CustomResourceDefinition serviceMonitorCRD = kubernetesClient.apiextensions().v1().customResourceDefinitions().withName(SERVICE_MONITOR_CRD_NAME).get();
        return serviceMonitorCRD != null;
    }
}
