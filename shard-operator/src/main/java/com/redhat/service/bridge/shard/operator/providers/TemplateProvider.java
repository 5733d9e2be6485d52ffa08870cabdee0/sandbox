package com.redhat.service.bridge.shard.operator.providers;

import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.openshift.api.model.Route;

public interface TemplateProvider {

    Deployment loadBridgeDeploymentTemplate(BridgeIngress bridgeIngress);

    Service loadBridgeServiceTemplate(BridgeIngress bridgeIngress);

    Route loadBridgeOpenshiftRouteTemplate(BridgeIngress bridgeIngress);

    Ingress loadBridgeKubernetesIngressTemplate(BridgeIngress bridgeIngress);
}
