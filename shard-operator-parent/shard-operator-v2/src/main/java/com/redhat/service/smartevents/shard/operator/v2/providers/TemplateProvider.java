package com.redhat.service.smartevents.shard.operator.v2.providers;

import com.redhat.service.smartevents.shard.operator.core.resources.istio.authorizationpolicy.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.gateway.Gateway;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.requestauthentication.RequestAuthentication;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.virtualservice.VirtualService;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

public interface TemplateProvider {

    Secret loadManagedBridgeSecretTemplate(ManagedBridge bridgeIngress, TemplateImportConfig config);

    Route loadManagedBridgeOpenshiftRouteTemplate(ManagedBridge bridgeIngress, TemplateImportConfig config);

    Ingress loadBridgeIngressKubernetesIngressTemplate(ManagedBridge bridgeIngress, TemplateImportConfig config);

    ServiceMonitor loadServiceMonitorTemplate(CustomResource resource, TemplateImportConfig config);

    KnativeBroker loadBridgeIngressBrokerTemplate(ManagedBridge managedBridge, TemplateImportConfig config);

    AuthorizationPolicy loadBridgeIngressAuthorizationPolicyTemplate(ManagedBridge managedBridge, TemplateImportConfig config);

    Gateway loadIstioGatewayTemplate();

    VirtualService loadIstioVirtualServiceTemplate();

    RequestAuthentication loadJWTRequestAuthenticationTemplate();
}
