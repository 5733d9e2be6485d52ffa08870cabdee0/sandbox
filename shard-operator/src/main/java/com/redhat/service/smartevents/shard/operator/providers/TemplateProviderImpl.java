package com.redhat.service.smartevents.shard.operator.providers;

import java.io.IOException;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.istio.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.resources.knative.KnativeBroker;

import io.fabric8.kubernetes.api.model.ConfigMap;
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

    @Override
    public Deployment loadBridgeExecutorDeploymentTemplate(BridgeExecutor bridgeExecutor, TemplateImportConfig config) {
        Deployment deployment = loadYaml(Deployment.class, BRIDGE_EXECUTOR_DEPLOYMENT_PATH);
        updateMetadata(bridgeExecutor, deployment.getMetadata(), config);
        return deployment;
    }

    @Override
    public Service loadBridgeExecutorServiceTemplate(BridgeExecutor bridgeExecutor, TemplateImportConfig config) {
        Service service = loadYaml(Service.class, BRIDGE_EXECUTOR_SERVICE_PATH);
        updateMetadata(bridgeExecutor, service.getMetadata(), config);
        return service;
    }

    @Override
    public Route loadBridgeIngressOpenshiftRouteTemplate(BridgeIngress bridgeIngress, TemplateImportConfig config) {
        Route route = loadYaml(Route.class, BRIDGE_INGRESS_OPENSHIFT_ROUTE_PATH);
        updateMetadata(bridgeIngress, route.getMetadata(), config);
        return route;
    }

    @Override
    public Ingress loadBridgeIngressKubernetesIngressTemplate(BridgeIngress bridgeIngress, TemplateImportConfig config) {
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
    public Secret loadBridgeIngressSecretTemplate(BridgeIngress bridgeIngress, TemplateImportConfig config) {
        final Secret secret = loadYaml(Secret.class, BRIDGE_INGRESS_SECRET_PATH);
        updateMetadata(bridgeIngress, secret.getMetadata(), config);
        return secret;
    }

    @Override
    public Secret loadBridgeExecutorSecretTemplate(BridgeExecutor bridgeExecutor, TemplateImportConfig config) {
        final Secret secret = loadYaml(Secret.class, BRIDGE_EXECUTOR_SECRET_PATH);
        updateMetadata(bridgeExecutor, secret.getMetadata(), config);
        return secret;
    }

    @Override
    public ConfigMap loadBridgeIngressConfigMapTemplate(BridgeIngress bridgeIngress, TemplateImportConfig config) {
        ConfigMap configMap = loadYaml(ConfigMap.class, BRIDGE_INGRESS_CONFIGMAP_PATH);
        updateMetadata(bridgeIngress, configMap.getMetadata(), config);
        return configMap;
    }

    @Override
    public KnativeBroker loadBridgeIngressBrokerTemplate(BridgeIngress bridgeIngress, TemplateImportConfig config) {
        KnativeBroker knativeBroker = loadYaml(KnativeBroker.class, BRIDGE_INGRESS_BROKER_PATH);
        updateMetadata(bridgeIngress, knativeBroker.getMetadata(), config);
        return knativeBroker;
    }

    @Override
    public AuthorizationPolicy loadBridgeIngressAuthorizationPolicyTemplate(BridgeIngress bridgeIngress, TemplateImportConfig config) {
        AuthorizationPolicy authorizationPolicy = loadYaml(AuthorizationPolicy.class, BRIDGE_INGRESS_AUTHORIZATION_POLICY_PATH);
        updateMetadata(bridgeIngress, authorizationPolicy.getMetadata(), config);
        return authorizationPolicy;
    }

    private <T> T loadYaml(Class<T> clazz, String yaml) {
        try (InputStream is = TemplateProviderImpl.class.getResourceAsStream(yaml)) {
            return Serialization.unmarshal(is, clazz);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot find yaml on classpath: " + yaml);
        }
    }

    private void updateMetadata(CustomResource resource, ObjectMeta meta, TemplateImportConfig config) {
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
    }
}
