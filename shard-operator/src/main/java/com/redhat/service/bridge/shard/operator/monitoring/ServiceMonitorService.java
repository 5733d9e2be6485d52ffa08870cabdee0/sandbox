package com.redhat.service.bridge.shard.operator.monitoring;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

/**
 * Service to handle the Prometheus extension resources.
 */
public interface ServiceMonitorService {

    /**
     * Fetch or create a new {@link ServiceMonitor} Prometheus Operator instance.
     *
     * @param service the target Kubernetes {@link Service} to bind the monitoring resource.
     * @param resource the custom resource managed by this operator (e.g. BridgeIngress).
     * @param componentName the name of the component created by this
     * @return an {@link Optional} {@link ServiceMonitor} if Prometheus Operator is installed in the cluster.
     */
    Optional<ServiceMonitor> fetchOrCreateServiceMonitor(final CustomResource resource, final Service service, final String componentName);
}
