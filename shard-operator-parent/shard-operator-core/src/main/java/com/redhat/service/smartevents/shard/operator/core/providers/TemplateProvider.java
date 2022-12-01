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
    Secret loadBridgeExecutorSecretTemplate(HasMetadata resource, TemplateImportConfig config);

    Deployment loadBridgeExecutorDeploymentTemplate(HasMetadata resource, TemplateImportConfig config);

    Service loadBridgeExecutorServiceTemplate(HasMetadata resource, TemplateImportConfig config);

    Route loadBridgeIngressOpenshiftRouteTemplate(HasMetadata resource, TemplateImportConfig config);

    Ingress loadBridgeIngressKubernetesIngressTemplate(HasMetadata resource, TemplateImportConfig config);

    ServiceMonitor loadServiceMonitorTemplate(CustomResource resource, TemplateImportConfig config);

    Secret loadBridgeIngressSecretTemplate(HasMetadata resource, TemplateImportConfig config);

    ConfigMap loadBridgeIngressConfigMapTemplate(HasMetadata resource, TemplateImportConfig config);

    KnativeBroker loadBridgeIngressBrokerTemplate(HasMetadata resource, TemplateImportConfig config);

    AuthorizationPolicy loadBridgeIngressAuthorizationPolicyTemplate(HasMetadata resource, TemplateImportConfig config);

    Gateway loadIstioGatewayTemplate();

    VirtualService loadIstioVirtualServiceTemplate();

    RequestAuthentication loadJWTRequestAuthenticationTemplate();
}
