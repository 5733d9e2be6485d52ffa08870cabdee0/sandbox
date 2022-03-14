package com.redhat.service.bridge.shard.operator.providers;

import com.redhat.service.bridge.shard.operator.resources.AuthorizationPolicy;
import com.redhat.service.bridge.shard.operator.resources.BridgeExecutor;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.resources.KnativeBroker;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

public interface TemplateProvider {

    // TODO: remove
    Deployment loadBridgeIngressDeploymentTemplate(BridgeIngress bridgeIngress);

    // TODO: remove
    Service loadBridgeIngressServiceTemplate(BridgeIngress bridgeIngress);

    ConfigMap loadBridgeIngressConfigMapTemplate(BridgeIngress bridgeIngress);

    KnativeBroker loadBridgeIngressBrokerTemplate(BridgeIngress bridgeIngress);

    AuthorizationPolicy loadBridgeIngressAuthorizationPolicyTemplate(BridgeIngress bridgeIngress);

    Deployment loadBridgeExecutorDeploymentTemplate(BridgeExecutor bridgeExecutor);

    Service loadBridgeExecutorServiceTemplate(BridgeExecutor bridgeExecutor);

    Route loadBridgeIngressOpenshiftRouteTemplate(BridgeIngress bridgeIngress);

    Ingress loadBridgeIngressKubernetesIngressTemplate(BridgeIngress bridgeIngress);

    ServiceMonitor loadServiceMonitorTemplate(CustomResource resource);

    Secret loadBridgeIngressSecretTemplate(BridgeIngress bridgeIngress);

    Secret loadBridgeExecutorSecretTemplate(BridgeExecutor bridgeExecutor);
}
