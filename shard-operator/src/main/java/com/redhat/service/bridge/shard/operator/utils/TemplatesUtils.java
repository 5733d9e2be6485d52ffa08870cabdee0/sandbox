package com.redhat.service.bridge.shard.operator.utils;

import java.io.IOException;
import java.io.InputStream;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;

public class TemplatesUtils {

    private static final String TEMPLATES_DIR = "/templates/";
    private static final String INGRESS_DEPLOYMENT_PATH = TEMPLATES_DIR + "bridge-ingress-deployment.yaml";

    public static Deployment loadIngressDeploymentTemplate(){
        return loadYaml(Deployment.class, INGRESS_DEPLOYMENT_PATH);
    }

    private static <T> T loadYaml(Class<T> clazz, String yaml) {
        try (InputStream is = TemplatesUtils.class.getResourceAsStream(yaml)) {
            return Serialization.unmarshal(is, clazz);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot find yaml on classpath: " + yaml);
        }
    }
}
