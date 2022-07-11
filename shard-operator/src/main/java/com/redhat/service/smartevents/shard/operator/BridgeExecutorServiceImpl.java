package com.redhat.service.smartevents.shard.operator;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.processors.Processing;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsProvider;
import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.camel.CamelIntegration;
import com.redhat.service.smartevents.shard.operator.utils.Constants;
import com.redhat.service.smartevents.shard.operator.utils.DeploymentSpecUtils;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class BridgeExecutorServiceImpl implements BridgeExecutorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeExecutorServiceImpl.class);

    public static final String KAFKA_ERROR_STRATEGY_IGNORE = "ignore";
    public static final String KAFKA_ERROR_STRATEGY_DLQ = "dead-letter-queue";

    @ConfigProperty(name = "event-bridge.executor.image")
    String executorImage;

    @ConfigProperty(name = "event-bridge.executor.deployment.timeout-seconds")
    int deploymentTimeout;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @Inject
    TemplateProvider templateProvider;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    GlobalConfigurationsProvider globalConfigurationsProvider;

    @Inject
    ManagerClient managerClient;

    @Override
    public void createBridgeExecutor(ProcessorDTO processorDTO) {
        final Namespace namespace = customerNamespaceProvider.fetchOrCreateCustomerNamespace(processorDTO.getCustomerId());

        BridgeExecutor expected = BridgeExecutor.fromDTO(processorDTO, namespace.getMetadata().getName(), executorImage);

        BridgeExecutor existing = kubernetesClient
                .resources(BridgeExecutor.class)
                .inNamespace(namespace.getMetadata().getName())
                .withName(BridgeExecutor.resolveResourceName(processorDTO.getId()))
                .get();

        if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            BridgeExecutor bridgeExecutor = kubernetesClient
                    .resources(BridgeExecutor.class)
                    .inNamespace(namespace.getMetadata().getName())
                    .createOrReplace(expected);

            // create or update the secrets for the bridgeExecutor
            createOrUpdateBridgeExecutorSecret(bridgeExecutor, processorDTO);
        }

        Processing processing = processorDTO.getDefinition().getProcessing();
        if (processing != null) {
            LOGGER.info("------ Creating a Camel Integration");

            CamelIntegration expectedIntegrationFromDTO = CamelIntegration.fromDTO(processorDTO, namespace.getMetadata().getName(), executorImage, processing);

            LOGGER.info("------ integration expected: " + expectedIntegrationFromDTO);

            CamelIntegration existingCamelIntegration = kubernetesClient
                    .resources(CamelIntegration.class)
                    .inNamespace(namespace.getMetadata().getName())
                    .withName(CamelIntegration.resolveResourceName(processorDTO.getId()))
                    .get();

            if (existingCamelIntegration == null || !expectedIntegrationFromDTO.getSpec().equals(existingCamelIntegration.getSpec())) {
                LOGGER.info("------ Integration not found, creating...");

                CamelIntegration createdResource = kubernetesClient
                        .resources(CamelIntegration.class)
                        .inNamespace(namespace.getMetadata().getName())
                        .createOrReplace(expectedIntegrationFromDTO);

                LOGGER.info("------ Created resource: " + createdResource);
            }
        }
    }

    @Override
    public Deployment fetchOrCreateBridgeExecutorDeployment(BridgeExecutor bridgeExecutor, Secret secret) {
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

        Deployment existing = kubernetesClient.apps().deployments().inNamespace(bridgeExecutor.getMetadata().getNamespace()).withName(bridgeExecutor.getMetadata().getName()).get();

        if (existing == null || !DeploymentSpecUtils.isDeploymentEqual(expected, existing)) {
            return kubernetesClient.apps().deployments().inNamespace(bridgeExecutor.getMetadata().getNamespace()).createOrReplace(expected);
        }

        return existing;
    }

    @Override
    public Service fetchOrCreateBridgeExecutorService(BridgeExecutor bridgeExecutor, Deployment deployment) {
        Service expected = templateProvider.loadBridgeExecutorServiceTemplate(bridgeExecutor, TemplateImportConfig.withDefaults());
        expected.getMetadata().getLabels().put(LabelsBuilder.INSTANCE_LABEL, deployment.getMetadata().getName());

        // Specs
        expected.getSpec().setSelector(new LabelsBuilder().withAppInstance(deployment.getMetadata().getName()).build());

        Service existing = kubernetesClient.services().inNamespace(bridgeExecutor.getMetadata().getNamespace()).withName(bridgeExecutor.getMetadata().getName()).get();

        if (existing == null || !expected.getSpec().getSelector().equals(existing.getSpec().getSelector())) {
            return kubernetesClient.services().inNamespace(bridgeExecutor.getMetadata().getNamespace()).createOrReplace(expected);
        }

        return existing;
    }

    @Override
    public void deleteBridgeExecutor(ProcessorDTO processorDTO) {
        final String namespace = customerNamespaceProvider.resolveName(processorDTO.getCustomerId());
        final boolean bridgeDeleted =
                kubernetesClient
                        .resources(BridgeExecutor.class)
                        .inNamespace(namespace)
                        .delete(BridgeExecutor.fromDTO(processorDTO, namespace, executorImage));
        if (!bridgeDeleted) {
            // TODO: we might need to review this use case and have a manager to look at a queue of objects not deleted and investigate. Unfortunately the API does not give us a reason.
            LOGGER.warn("BridgeExecutor '{}' not deleted. Notifying manager that it has been deleted.", processorDTO.getId());
            processorDTO.setStatus(ManagedResourceStatus.DELETED);
            managerClient.notifyProcessorStatusChange(processorDTO).subscribe().with(
                    success -> LOGGER.debug("Deleted notification for BridgeExecutor '{}' has been sent to the manager successfully", processorDTO.getId()),
                    failure -> LOGGER.error("Failed to send updated status to Manager for entity of type '{}'", ProcessorDTO.class.getSimpleName(), failure));
        }

        Processing processing = processorDTO.getDefinition().getProcessing();
        if (processing != null) {
            LOGGER.info("------ Processing found - Deleting the Camel Integration");

            String camelResourceName = CamelIntegration.resolveResourceName(processorDTO.getId());
            CamelIntegration expectedIntegrationFromDTO = CamelIntegration.fromDTO(processorDTO, namespace, executorImage, processing);

            LOGGER.info("------ Deleting {} with definition {}", camelResourceName, expectedIntegrationFromDTO);

            final boolean camelIntegrationDeleted =
                    kubernetesClient
                            .resources(CamelIntegration.class)
                            .inNamespace(namespace)
                            .delete(expectedIntegrationFromDTO);

            if(camelIntegrationDeleted) {
                LOGGER.info("------ Deleted");
            } else {
                LOGGER.info("------ NOT Deleted");
            }
        }
    }

    @Override
    public void createOrUpdateBridgeExecutorSecret(BridgeExecutor bridgeExecutor, ProcessorDTO processorDTO) {
        String kafkaErrorStrategy = processorDTO.getType() == ProcessorType.ERROR_HANDLER
                ? KAFKA_ERROR_STRATEGY_IGNORE
                : KAFKA_ERROR_STRATEGY_DLQ;

        Secret expected = templateProvider.loadBridgeExecutorSecretTemplate(bridgeExecutor, TemplateImportConfig.withDefaults());

        expected.getData().put(GlobalConfigurationsConstants.KAFKA_BOOTSTRAP_SERVERS_ENV_VAR, Base64.getEncoder().encodeToString(processorDTO.getKafkaConnection().getBootstrapServers().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_CLIENT_ID_ENV_VAR, Base64.getEncoder().encodeToString(processorDTO.getKafkaConnection().getClientId().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_CLIENT_SECRET_ENV_VAR, Base64.getEncoder().encodeToString(processorDTO.getKafkaConnection().getClientSecret().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_SECURITY_PROTOCOL_ENV_VAR, Base64.getEncoder().encodeToString(processorDTO.getKafkaConnection().getSecurityProtocol().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_TOPIC_ENV_VAR, Base64.getEncoder().encodeToString(processorDTO.getKafkaConnection().getTopic().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_ERROR_STRATEGY_ENV_VAR, Base64.getEncoder().encodeToString(kafkaErrorStrategy.getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_ERROR_TOPIC_ENV_VAR, Base64.getEncoder().encodeToString(processorDTO.getKafkaConnection().getErrorTopic().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_GROUP_ID_ENV_VAR, Base64.getEncoder().encodeToString(bridgeExecutor.getSpec().getId().getBytes()));

        Secret existing = kubernetesClient
                .secrets()
                .inNamespace(bridgeExecutor.getMetadata().getNamespace())
                .withName(bridgeExecutor.getMetadata().getName())
                .get();

        if (existing == null || !expected.getData().equals(existing.getData())) {
            kubernetesClient
                    .secrets()
                    .inNamespace(bridgeExecutor.getMetadata().getNamespace())
                    .withName(bridgeExecutor.getMetadata().getName())
                    .createOrReplace(expected);
        }
    }

    @Override
    public Secret fetchBridgeExecutorSecret(BridgeExecutor bridgeExecutor) {
        return kubernetesClient
                .secrets()
                .inNamespace(bridgeExecutor.getMetadata().getNamespace())
                .withName(bridgeExecutor.getMetadata().getName())
                .get();
    }

    @Override
    public String getExecutorImage() {
        return executorImage;
    }
}
