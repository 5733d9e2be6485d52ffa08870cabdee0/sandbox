package com.redhat.service.smartevents.shard.operator.core;

import com.redhat.service.smartevents.shard.operator.core.resources.istio.requestauthentication.RequestAuthentication;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;

import java.io.IOException;
import java.io.InputStream;

public class TemplateProvider {

    private static final String TEMPLATES_DIR = "/templates";
    private static final String TEST_DEPLOYMENT_PATH = TEMPLATES_DIR + "/test_deployment.yaml";

    public static Deployment loadTestDeployment(){
        return loadYaml(Deployment.class, TEST_DEPLOYMENT_PATH);
    }

    private static <T> T loadYaml(Class<T> clazz, String yaml) {
        try (InputStream is = TemplateProvider.class.getResourceAsStream(yaml)) {
            return Serialization.unmarshal(is, clazz);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot find yaml on classpath: " + yaml);
        }
    }
}
