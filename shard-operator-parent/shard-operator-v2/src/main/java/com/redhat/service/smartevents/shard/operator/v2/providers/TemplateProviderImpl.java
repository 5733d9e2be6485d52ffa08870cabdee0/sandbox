package com.redhat.service.smartevents.shard.operator.v2.providers;

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
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

@ApplicationScoped
public class TemplateProviderImpl implements TemplateProvider {

    private static final String TEMPLATES_DIR = "/templates";

    private static final String MANAGED_BRIDGE_SECRET_PATH = TEMPLATES_DIR + "/managed-bridge-secret.yaml";

    @Override
    public Secret loadManagedBridgeSecretTemplate(ManagedBridge bridgeIngress, TemplateImportConfig config) {
        final Secret secret = loadYaml(Secret.class, MANAGED_BRIDGE_SECRET_PATH);
        updateMetadata(bridgeIngress, secret.getMetadata(), config);
        return secret;
    }

    private void updateMetadata(HasMetadata resource, ObjectMeta meta, TemplateImportConfig config) {
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

    private <T> T loadYaml(Class<T> clazz, String yaml) {
        try (InputStream is = TemplateProviderImpl.class.getResourceAsStream(yaml)) {
            return Serialization.unmarshal(is, clazz);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot find yaml on classpath: " + yaml);
        }
    }

    @Override
    public Route loadManagedBridgeOpenshiftRouteTemplate(ManagedBridge bridgeIngress, TemplateImportConfig config) {
        return null;
    }

    @Override
    public Ingress loadBridgeIngressKubernetesIngressTemplate(ManagedBridge bridgeIngress, TemplateImportConfig config) {
        return null;
    }

    @Override
    public ServiceMonitor loadServiceMonitorTemplate(CustomResource resource, TemplateImportConfig config) {
        return null;
    }

    @Override
    public KnativeBroker loadBridgeIngressBrokerTemplate(ManagedBridge managedBridge, TemplateImportConfig config) {
        return null;
    }

    @Override
    public AuthorizationPolicy loadBridgeIngressAuthorizationPolicyTemplate(ManagedBridge managedBridge, TemplateImportConfig config) {
        return null;
    }

    @Override
    public Gateway loadIstioGatewayTemplate() {
        return null;
    }

    @Override
    public VirtualService loadIstioVirtualServiceTemplate() {
        return null;
    }

    @Override
    public RequestAuthentication loadJWTRequestAuthenticationTemplate() {
        return null;
    }
}
