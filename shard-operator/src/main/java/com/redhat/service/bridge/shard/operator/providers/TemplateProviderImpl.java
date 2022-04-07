package com.redhat.service.bridge.shard.operator.providers;

import java.io.IOException;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.shard.operator.resources.BridgeExecutor;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.resources.istio.AuthorizationPolicy;
import com.redhat.service.bridge.shard.operator.resources.knative.KnativeBroker;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

@ApplicationScoped
public class TemplateProviderImpl implements TemplateProvider {

    private static final String TEMPLATES_DIR = "/templates";
    private static final String BRIDGE_INGRESS_SECRET_PATH = TEMPLATES_DIR + "/bridge-ingress-secret.yaml";
    private static final String BRIDGE_INGRESS_CONFIGMAP_PATH = TEMPLATES_DIR + "/bridge-ingress-broker-configmap.yaml";
    private static final String BRIDGE_INGRESS_BROKER_PATH = TEMPLATES_DIR + "/bridge-ingress-broker.yaml";
    private static final String BRIDGE_INGRESS_AUTHORIZATION_POLICY_PATH = TEMPLATES_DIR + "/bridge-ingress-authorization-policy.yaml";

    private static final String BRIDGE_EXECUTOR_DEPLOYMENT_PATH = TEMPLATES_DIR + "/bridge-executor-deployment.yaml";
    private static final String BRIDGE_EXECUTOR_SERVICE_PATH = TEMPLATES_DIR + "/bridge-executor-service.yaml";
    private static final String BRIDGE_EXECUTOR_SECRET_PATH = TEMPLATES_DIR + "/bridge-executor-secret.yaml";

    private static final String SERVICE_MONITOR_PATH = TEMPLATES_DIR + "/service-monitor.yaml";

    @Override
    public Deployment loadBridgeExecutorDeploymentTemplate(BridgeExecutor bridgeExecutor) {
        Deployment deployment = loadYaml(Deployment.class, BRIDGE_EXECUTOR_DEPLOYMENT_PATH);
        updateMetadata(bridgeExecutor, deployment.getMetadata());
        return deployment;
    }

    @Override
    public Service loadBridgeExecutorServiceTemplate(BridgeExecutor bridgeExecutor) {
        Service service = loadYaml(Service.class, BRIDGE_EXECUTOR_SERVICE_PATH);
        updateMetadata(bridgeExecutor, service.getMetadata());
        return service;
    }

    @Override
    public ServiceMonitor loadServiceMonitorTemplate(CustomResource resource) {
        final ServiceMonitor serviceMonitor = loadYaml(ServiceMonitor.class, SERVICE_MONITOR_PATH);
        updateMetadata(resource, serviceMonitor.getMetadata());
        return serviceMonitor;
    }

    @Override
    public Secret loadBridgeIngressSecretTemplate(BridgeIngress bridgeIngress) {
        final Secret secret = loadYaml(Secret.class, BRIDGE_INGRESS_SECRET_PATH);
        updateMetadata(bridgeIngress, secret.getMetadata());
        return secret;
    }

    @Override
    public Secret loadBridgeExecutorSecretTemplate(BridgeExecutor bridgeExecutor) {
        final Secret secret = loadYaml(Secret.class, BRIDGE_EXECUTOR_SECRET_PATH);
        updateMetadata(bridgeExecutor, secret.getMetadata());
        return secret;
    }

    @Override
    public ConfigMap loadBridgeIngressConfigMapTemplate(BridgeIngress bridgeIngress) {
        ConfigMap configMap = loadYaml(ConfigMap.class, BRIDGE_INGRESS_CONFIGMAP_PATH);
        updateMetadata(bridgeIngress, configMap.getMetadata());
        return configMap;
    }

    @Override
    public KnativeBroker loadBridgeIngressBrokerTemplate(BridgeIngress bridgeIngress) {
        KnativeBroker knativeBroker = loadYaml(KnativeBroker.class, BRIDGE_INGRESS_BROKER_PATH);
        updateMetadata(bridgeIngress, knativeBroker.getMetadata());
        return knativeBroker;
    }

    @Override
    public AuthorizationPolicy loadBridgeIngressAuthorizationPolicyTemplate(BridgeIngress bridgeIngress) {
        AuthorizationPolicy authorizationPolicy = loadYaml(AuthorizationPolicy.class, BRIDGE_INGRESS_AUTHORIZATION_POLICY_PATH);
        updateMetadata(bridgeIngress, authorizationPolicy.getMetadata());
        authorizationPolicy.getMetadata().setNamespace("istio-system"); // https://github.com/istio/istio/issues/37221
        authorizationPolicy.getMetadata().setOwnerReferences(null); // TODO: we have to manually delete this in the delete hook of the reconciler
        return authorizationPolicy;
    }

    private <T> T loadYaml(Class<T> clazz, String yaml) {
        try (InputStream is = TemplateProviderImpl.class.getResourceAsStream(yaml)) {
            return Serialization.unmarshal(is, clazz);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot find yaml on classpath: " + yaml);
        }
    }

    private void updateMetadata(CustomResource resource, ObjectMeta meta) {
        // Name and namespace
        meta.setName(resource.getMetadata().getName());
        meta.setNamespace(resource.getMetadata().getNamespace());

        // Owner reference
        meta.getOwnerReferences().get(0).setKind(resource.getKind());
        meta.getOwnerReferences().get(0).setName(resource.getMetadata().getName());
        meta.getOwnerReferences().get(0).setApiVersion(resource.getApiVersion());
        meta.getOwnerReferences().get(0).setUid(resource.getMetadata().getUid());
    }
}
