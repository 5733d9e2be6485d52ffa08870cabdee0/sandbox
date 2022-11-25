package com.redhat.service.smartevents.shard.operator.core.providers;

import com.redhat.service.smartevents.shard.operator.core.resources.istio.authorizationpolicy.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.gateway.Gateway;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.requestauthentication.RequestAuthentication;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.virtualservice.VirtualService;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBroker;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

public interface TemplateProvider {
    Secret loadBridgeExecutorSecretTemplate(HasMetadata bridgeExecutor, TemplateImportConfig config);

    Deployment loadBridgeExecutorDeploymentTemplate(HasMetadata bridgeExecutor, TemplateImportConfig config);

    Service loadBridgeExecutorServiceTemplate(HasMetadata bridgeExecutor, TemplateImportConfig config);

    Route loadBridgeIngressOpenshiftRouteTemplate(HasMetadata bridgeIngress, TemplateImportConfig config);

    Ingress loadBridgeIngressKubernetesIngressTemplate(HasMetadata bridgeIngress, TemplateImportConfig config);

    ServiceMonitor loadServiceMonitorTemplate(CustomResource resource, TemplateImportConfig config);

    Secret loadBridgeIngressSecretTemplate(HasMetadata bridgeIngress, TemplateImportConfig config);

    ConfigMap loadBridgeIngressConfigMapTemplate(HasMetadata bridgeIngress, TemplateImportConfig config);

    KnativeBroker loadBridgeIngressBrokerTemplate(HasMetadata bridgeIngress, TemplateImportConfig config);

    AuthorizationPolicy loadBridgeIngressAuthorizationPolicyTemplate(HasMetadata bridgeIngress, TemplateImportConfig config);

    Gateway loadIstioGatewayTemplate();

    VirtualService loadIstioVirtualServiceTemplate();

    RequestAuthentication loadJWTRequestAuthenticationTemplate();
}
