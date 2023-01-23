package com.redhat.service.smartevents.shard.operator.v1.providers;

import com.redhat.service.smartevents.shard.operator.core.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateProvider;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

public interface TemplateProviderV1 extends TemplateProvider {
    Secret loadBridgeExecutorSecretTemplate(HasMetadata resource, TemplateImportConfig config);

    Deployment loadBridgeExecutorDeploymentTemplate(HasMetadata resource, TemplateImportConfig config);

    Service loadBridgeExecutorServiceTemplate(HasMetadata resource, TemplateImportConfig config);

    ServiceMonitor loadServiceMonitorTemplate(CustomResource resource, TemplateImportConfig config);

}