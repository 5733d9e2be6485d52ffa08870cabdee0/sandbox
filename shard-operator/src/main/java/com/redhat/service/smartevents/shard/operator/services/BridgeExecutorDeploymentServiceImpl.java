package com.redhat.service.smartevents.shard.operator.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.smartevents.shard.operator.BridgeExecutorServiceImpl;
import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsProvider;
import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.utils.Constants;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class BridgeExecutorDeploymentServiceImpl implements BridgeExecutorDeploymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeExecutorDeploymentServiceImpl.class);

    @ConfigProperty(name = "event-bridge.executor.deployment.timeout-seconds")
    int deploymentTimeout;

    @Inject
    TemplateProvider templateProvider;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    GlobalConfigurationsProvider globalConfigurationsProvider;

    @Override
    public Deployment createBridgeExecutorDeployment(BridgeExecutor bridgeExecutor) {
        Deployment expected = templateProvider.loadBridgeExecutorDeploymentTemplate(bridgeExecutor, TemplateImportConfig.withDefaults());

        // Specs
        expected.getSpec().getSelector().setMatchLabels(new LabelsBuilder().withAppInstance(bridgeExecutor.getMetadata().getName()).build());
        expected.getSpec().getTemplate().getMetadata().setLabels(new LabelsBuilder().withAppInstance(bridgeExecutor.getMetadata().getName()).build());
        expected.getSpec().getTemplate().getSpec().getContainers().get(0).setName(BridgeExecutor.COMPONENT_NAME);
        expected.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(bridgeExecutor.getSpec().getImage());
        expected.getSpec().setProgressDeadlineSeconds(deploymentTimeout);

        List<EnvVar> environmentVariables = new ArrayList<>();
        environmentVariables.add(new EnvVarBuilder().withName(Constants.BRIDGE_EXECUTOR_WEBHOOK_SSO_ENV_VAR).withValue(globalConfigurationsProvider.getSsoUrl()).build());
        environmentVariables.add(new EnvVarBuilder().withName(Constants.BRIDGE_EXECUTOR_WEBHOOK_CLIENT_ID_ENV_VAR).withValue(globalConfigurationsProvider.getSsoWebhookClientId()).build());
        environmentVariables.add(new EnvVarBuilder().withName(Constants.BRIDGE_EXECUTOR_WEBHOOK_CLIENT_SECRET_ENV_VAR).withValue(globalConfigurationsProvider.getSsoWebhookClientSecret()).build());
        // CustomerId is available in the Processor definition however this avoids the need to unnecessarily de-serialise the definition for logging in the Executor
        environmentVariables.add(new EnvVarBuilder().withName(Constants.CUSTOMER_ID_CONFIG_ENV_VAR).withValue(bridgeExecutor.getSpec().getCustomerId()).build());
        environmentVariables.add(new EnvVarBuilder().withName(Constants.EVENT_BRIDGE_LOGGING_JSON).withValue(globalConfigurationsProvider.isJsonLoggingEnabled().toString()).build());
        try {
            environmentVariables.add(new EnvVarBuilder().withName(Constants.BRIDGE_EXECUTOR_PROCESSOR_DEFINITION_ENV_VAR).withValue(objectMapper.writeValueAsString(bridgeExecutor.toDTO())).build());
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not serialize Processor Definition while setting executor deployment environment variables", e);
        }
        expected.getSpec().getTemplate().getSpec().getContainers().get(0).setEnv(environmentVariables);

        expected.getSpec().getTemplate().getSpec().getContainers().get(0).getEnvFrom().get(0).getSecretRef().setName(secret.getMetadata().getName());
        return expected;
    }

    @Override
    public Deployment fetchBridgeExecutorDeployment(BridgeExecutor bridgeExecutor) {
        return kubernetesClient
                .apps().deployments()
                .inNamespace(bridgeExecutor.getMetadata().getNamespace())
                .withName(bridgeExecutor.getMetadata().getName())
                .get();
    }
}
