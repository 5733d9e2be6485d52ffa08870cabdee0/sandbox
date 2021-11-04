package com.redhat.service.bridge.shard.operator.providers;

import java.io.IOException;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;

@ApplicationScoped
public class TemplateProviderImpl implements TemplateProvider {
    private static final String TEMPLATES_DIR = "/templates/";
    private static final String INGRESS_DEPLOYMENT_PATH = TEMPLATES_DIR + "bridge-ingress-deployment.yaml";

    @Override
    public Deployment loadIngressDeploymentTemplate() {
        return loadYaml(Deployment.class, INGRESS_DEPLOYMENT_PATH);
    }

    private <T> T loadYaml(Class<T> clazz, String yaml) {
        try (InputStream is = TemplateProviderImpl.class.getResourceAsStream(yaml)) {
            return Serialization.unmarshal(is, clazz);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot find yaml on classpath: " + yaml);
        }
    }
}
