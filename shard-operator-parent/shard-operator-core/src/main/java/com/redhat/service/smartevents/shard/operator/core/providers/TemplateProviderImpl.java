package com.redhat.service.smartevents.shard.operator.core.providers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.shard.operator.core.resources.istio.authorizationpolicy.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.gateway.Gateway;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.requestauthentication.RequestAuthentication;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.virtualservice.VirtualService;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

@ApplicationScoped
public class TemplateProviderImpl implements TemplateProvider {

    private static final String TEMPLATES_DIR = "/templates";
    private static final String BRIDGE_INGRESS_OPENSHIFT_ROUTE_PATH = TEMPLATES_DIR + "/bridge-ingress-openshift-route.yaml";
    private static final String BRIDGE_INGRESS_KUBERNETES_INGRESS_PATH = TEMPLATES_DIR + "/bridge-ingress-kubernetes-ingress.yaml";
    private static final String BRIDGE_INGRESS_SECRET_PATH = TEMPLATES_DIR + "/bridge-ingress-secret.yaml";
    private static final String BRIDGE_INGRESS_CONFIGMAP_PATH = TEMPLATES_DIR + "/bridge-ingress-broker-configmap.yaml";
    private static final String BRIDGE_INGRESS_BROKER_PATH = TEMPLATES_DIR + "/bridge-ingress-broker.yaml";
    private static final String BRIDGE_INGRESS_AUTHORIZATION_POLICY_PATH = TEMPLATES_DIR + "/bridge-ingress-authorization-policy.yaml";

    private static final String BRIDGE_EXECUTOR_DEPLOYMENT_PATH = TEMPLATES_DIR + "/bridge-executor-deployment.yaml";
    private static final String BRIDGE_EXECUTOR_SERVICE_PATH = TEMPLATES_DIR + "/bridge-executor-service.yaml";
    private static final String BRIDGE_EXECUTOR_SECRET_PATH = TEMPLATES_DIR + "/bridge-executor-secret.yaml";

    private static final String SERVICE_MONITOR_PATH = TEMPLATES_DIR + "/service-monitor.yaml";

    private static final String ISTIO_GATEWAY_PATH = TEMPLATES_DIR + "/gateway.yaml";

    private static final String ISTIO_VIRTUAL_SERVICE_PATH = TEMPLATES_DIR + "/virtual-service-kafka-broker.yaml";

    private static final String JWT_REQUEST_AUTHENTICATION_PATH = TEMPLATES_DIR + "/jwt-request-authentication.yaml";

    @Override
    public Deployment loadBridgeExecutorDeploymentTemplate(HasMetadata bridgeExecutor, TemplateImportConfig config) {
        Deployment deployment = loadYaml(Deployment.class, BRIDGE_EXECUTOR_DEPLOYMENT_PATH);
        updateMetadata(bridgeExecutor, deployment.getMetadata(), config);
        return deployment;
    }

    @Override
    public Service loadBridgeExecutorServiceTemplate(HasMetadata bridgeExecutor, TemplateImportConfig config) {
        Service service = loadYaml(Service.class, BRIDGE_EXECUTOR_SERVICE_PATH);
        updateMetadata(bridgeExecutor, service.getMetadata(), config);
        return service;
    }

    @Override
    public Route loadBridgeIngressOpenshiftRouteTemplate(HasMetadata bridgeIngress, TemplateImportConfig config) {
        Route route = loadYaml(Route.class, BRIDGE_INGRESS_OPENSHIFT_ROUTE_PATH);
        updateMetadata(bridgeIngress, route.getMetadata(), config);
        return route;
    }

    @Override
    public Ingress loadBridgeIngressKubernetesIngressTemplate(HasMetadata bridgeIngress, TemplateImportConfig config) {
        Ingress ingress = loadYaml(Ingress.class, BRIDGE_INGRESS_KUBERNETES_INGRESS_PATH);
        updateMetadata(bridgeIngress, ingress.getMetadata(), config);
        return ingress;
    }

    @Override
    public ServiceMonitor loadServiceMonitorTemplate(CustomResource resource, TemplateImportConfig config) {
        final ServiceMonitor serviceMonitor = loadYaml(ServiceMonitor.class, SERVICE_MONITOR_PATH);
        updateMetadata(resource, serviceMonitor.getMetadata(), config);
        return serviceMonitor;
    }

    @Override
    public Secret loadBridgeIngressSecretTemplate(HasMetadata bridgeIngress, TemplateImportConfig config) {
        final Secret secret = loadYaml(Secret.class, BRIDGE_INGRESS_SECRET_PATH);
        updateMetadata(bridgeIngress, secret.getMetadata(), config);
        return secret;
    }

    @Override
    public Secret loadBridgeExecutorSecretTemplate(HasMetadata bridgeExecutor, TemplateImportConfig config) {
        final Secret secret = loadYaml(Secret.class, BRIDGE_EXECUTOR_SECRET_PATH);
        updateMetadata(bridgeExecutor, secret.getMetadata(), config);
        return secret;
    }

    @Override
    public ConfigMap loadBridgeIngressConfigMapTemplate(HasMetadata bridgeIngress, TemplateImportConfig config) {
        ConfigMap configMap = loadYaml(ConfigMap.class, BRIDGE_INGRESS_CONFIGMAP_PATH);
        updateMetadata(bridgeIngress, configMap.getMetadata(), config);
        return configMap;
    }

    @Override
    public KnativeBroker loadBridgeIngressBrokerTemplate(HasMetadata bridgeIngress, TemplateImportConfig config) {
        KnativeBroker knativeBroker = loadYaml(KnativeBroker.class, BRIDGE_INGRESS_BROKER_PATH);
        updateMetadata(bridgeIngress, knativeBroker.getMetadata(), config);
        return knativeBroker;
    }

    @Override
    public AuthorizationPolicy loadBridgeIngressAuthorizationPolicyTemplate(HasMetadata bridgeIngress, TemplateImportConfig config) {
        AuthorizationPolicy authorizationPolicy = loadYaml(AuthorizationPolicy.class, BRIDGE_INGRESS_AUTHORIZATION_POLICY_PATH);
        updateMetadata(bridgeIngress, authorizationPolicy.getMetadata(), config);
        return authorizationPolicy;
    }

    @Override
    public Gateway loadIstioGatewayTemplate() {
        return loadYaml(Gateway.class, ISTIO_GATEWAY_PATH);
    }

    @Override
    public VirtualService loadIstioVirtualServiceTemplate() {
        return loadYaml(VirtualService.class, ISTIO_VIRTUAL_SERVICE_PATH);
    }

    @Override
    public RequestAuthentication loadJWTRequestAuthenticationTemplate() {
        return loadYaml(RequestAuthentication.class, JWT_REQUEST_AUTHENTICATION_PATH);
    }

    private <T> T loadYaml(Class<T> clazz, String yaml) {
        try (InputStream is = TemplateProviderImpl.class.getResourceAsStream(yaml)) {
            return Serialization.unmarshal(is, clazz);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot find yaml on classpath: " + yaml);
        }
    }

    private void updateMetadata(HasMetadata resource, ObjectMeta meta, TemplateImportConfig config) {

        // Set the owning operatorName
        Map<String, String> labels = new LabelsBuilder().buildWithDefaults(config.getOperatorName());
        if (meta.getLabels() == null) {
            meta.setLabels(labels);
        } else {
            meta.getLabels().putAll(labels);
        }

        // Name and namespace
        meta.setName(config.isNameToBeSet() ? resource.getMetadata().getName() : null);
        meta.setNamespace(config.isNamespaceToBeSet() ? resource.getMetadata().getNamespace() : null);

        // Owner reference
        if (config.isOwnerReferencesToBeSet()) {
            meta.getOwnerReferences().get(0).setKind(resource.getKind());
            meta.getOwnerReferences().get(0).setName(resource.getMetadata().getName());
            meta.getOwnerReferences().get(0).setApiVersion(resource.getApiVersion());
            meta.getOwnerReferences().get(0).setUid(resource.getMetadata().getUid());
        } else {
            meta.setOwnerReferences(null);
        }

        // Primary resource
        if (config.isPrimaryResourceToBeSet()) {
            Map<String, String> annotations = new LabelsBuilder()
                    .withPrimaryResourceName(resource.getMetadata().getName())
                    .withPrimaryResourceNamespace(resource.getMetadata().getNamespace())
                    .build();
            if (meta.getAnnotations() == null) {
                meta.setAnnotations(annotations);
            } else {
                meta.getAnnotations().putAll(annotations);
            }
        }
    }
}
