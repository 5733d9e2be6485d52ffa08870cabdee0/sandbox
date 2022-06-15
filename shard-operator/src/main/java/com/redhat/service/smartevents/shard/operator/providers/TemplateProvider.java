package com.redhat.service.smartevents.shard.operator.providers;

import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.istio.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.resources.knative.KnativeBroker;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

public interface TemplateProvider {
    Secret loadBridgeExecutorSecretTemplate(BridgeExecutor bridgeExecutor, TemplateImportConfig config);

    Deployment loadBridgeExecutorDeploymentTemplate(BridgeExecutor bridgeExecutor, TemplateImportConfig config);

    Service loadBridgeExecutorServiceTemplate(BridgeExecutor bridgeExecutor, TemplateImportConfig config);

    Route loadBridgeIngressOpenshiftRouteTemplate(BridgeIngress bridgeIngress, TemplateImportConfig config);

    Ingress loadBridgeIngressKubernetesIngressTemplate(BridgeIngress bridgeIngress, TemplateImportConfig config);

    ServiceMonitor loadServiceMonitorTemplate(CustomResource resource, TemplateImportConfig config);

    Secret loadBridgeIngressSecretTemplate(BridgeIngress bridgeIngress, TemplateImportConfig config);

    ConfigMap loadBridgeIngressConfigMapTemplate(BridgeIngress bridgeIngress, TemplateImportConfig config);

    KnativeBroker loadBridgeIngressBrokerTemplate(BridgeIngress bridgeIngress, TemplateImportConfig config);

    AuthorizationPolicy loadBridgeIngressAuthorizationPolicyTemplate(BridgeIngress bridgeIngress, TemplateImportConfig config);
}
