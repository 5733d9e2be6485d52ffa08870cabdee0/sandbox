package com.redhat.service.smartevents.shard.operator.v1;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.redhat.service.smartevents.shard.operator.v1.monitoring.ServiceMonitorClient;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;

import static com.redhat.service.smartevents.shard.operator.v1.monitoring.ServiceMonitorClient.SERVICE_MONITOR_CRD_NAME;

/**
 * Test cases that requires Prometheus should inherit from this base Test Case
 */
@WithPrometheus
@Interceptor
@Priority(1000)
public class PrometheusRegisterInterceptor {

    @Inject
    KubernetesClient kubernetesClient;

    @AroundInvoke
    public Object registerPrometheus(InvocationContext context) throws Exception {
        if (!ServiceMonitorClient.isServiceMonitorAvailable(kubernetesClient)) {
            final CustomResourceDefinition serviceMonitorCRD =
                    kubernetesClient.apiextensions().v1().customResourceDefinitions().load(this.getClass().getResourceAsStream("/k8s/servicemonitor.v1.crd.yaml")).get();
            kubernetesClient.apiextensions().v1().customResourceDefinitions().withName(SERVICE_MONITOR_CRD_NAME).create(serviceMonitorCRD);
        }

        return context.proceed();
    }
}
