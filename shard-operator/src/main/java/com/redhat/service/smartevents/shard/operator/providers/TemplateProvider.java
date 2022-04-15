package com.redhat.service.smartevents.shard.operator.providers;

import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

public interface TemplateProvider {

    Deployment loadBridgeIngressDeploymentTemplate(BridgeIngress bridgeIngress);

    Service loadBridgeIngressServiceTemplate(BridgeIngress bridgeIngress);

    Deployment loadBridgeExecutorDeploymentTemplate(BridgeExecutor bridgeExecutor);

    Service loadBridgeExecutorServiceTemplate(BridgeExecutor bridgeExecutor);

    Route loadBridgeIngressOpenshiftRouteTemplate(BridgeIngress bridgeIngress);

    Ingress loadBridgeIngressKubernetesIngressTemplate(BridgeIngress bridgeIngress);

    ServiceMonitor loadServiceMonitorTemplate(CustomResource resource);

    Secret loadBridgeIngressSecretTemplate(BridgeIngress bridgeIngress);

    Secret loadBridgeExecutorSecretTemplate(BridgeExecutor bridgeExecutor);
}
