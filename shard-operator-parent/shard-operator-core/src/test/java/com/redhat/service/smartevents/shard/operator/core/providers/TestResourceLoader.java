package com.redhat.service.smartevents.shard.operator.core.providers;

import io.fabric8.kubernetes.api.model.apps.Deployment;

public class TestResourceLoader {

    private static final String TEMPLATES_DIR = "/templates";
    private static final String TEST_DEPLOYMENT_PATH = TEMPLATES_DIR + "/test_deployment.yaml";

    public static Deployment loadTestDeployment() {
        TemplateProviderImpl templateProvider = new TemplateProviderImpl();
        return templateProvider.loadYaml(Deployment.class, TEST_DEPLOYMENT_PATH);
    }
}
