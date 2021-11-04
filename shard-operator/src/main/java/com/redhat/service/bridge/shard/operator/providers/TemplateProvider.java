package com.redhat.service.bridge.shard.operator.providers;

import io.fabric8.kubernetes.api.model.apps.Deployment;

public interface TemplateProvider {
    Deployment loadIngressDeploymentTemplate();
}
