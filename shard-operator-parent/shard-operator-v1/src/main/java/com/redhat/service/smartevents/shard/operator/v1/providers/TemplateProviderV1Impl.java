package com.redhat.service.smartevents.shard.operator.v1.providers;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.v1.api.V1;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateProviderImpl;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

@V1
@ApplicationScoped
public class TemplateProviderV1Impl extends TemplateProviderImpl implements TemplateProviderV1 {

    private static final String TEMPLATES_DIR = "/templates";

    private static final String BRIDGE_EXECUTOR_DEPLOYMENT_PATH = TEMPLATES_DIR + "/bridge-executor-deployment.yaml";
    private static final String BRIDGE_EXECUTOR_SERVICE_PATH = TEMPLATES_DIR + "/bridge-executor-service.yaml";
    private static final String BRIDGE_EXECUTOR_SECRET_PATH = TEMPLATES_DIR + "/bridge-executor-secret.yaml";

    private static final String SERVICE_MONITOR_PATH = TEMPLATES_DIR + "/service-monitor.yaml";

    @Override
    public Deployment loadBridgeExecutorDeploymentTemplate(HasMetadata resource, TemplateImportConfig config) {
        Deployment deployment = loadYaml(Deployment.class, BRIDGE_EXECUTOR_DEPLOYMENT_PATH);
        updateMetadata(resource, deployment.getMetadata(), config);
        return deployment;
    }

    @Override
    public Service loadBridgeExecutorServiceTemplate(HasMetadata resource, TemplateImportConfig config) {
        Service service = loadYaml(Service.class, BRIDGE_EXECUTOR_SERVICE_PATH);
        updateMetadata(resource, service.getMetadata(), config);
        return service;
    }

    @Override
    public ServiceMonitor loadServiceMonitorTemplate(CustomResource resource, TemplateImportConfig config) {
        final ServiceMonitor serviceMonitor = loadYaml(ServiceMonitor.class, SERVICE_MONITOR_PATH);
        updateMetadata(resource, serviceMonitor.getMetadata(), config);
        return serviceMonitor;
    }

    @Override
    public Secret loadBridgeExecutorSecretTemplate(HasMetadata resource, TemplateImportConfig config) {
        final Secret secret = loadYaml(Secret.class, BRIDGE_EXECUTOR_SECRET_PATH);
        updateMetadata(resource, secret.getMetadata(), config);
        return secret;
    }

}
