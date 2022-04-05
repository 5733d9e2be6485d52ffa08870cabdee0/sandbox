package com.redhat.service.bridge.shard.operator;

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
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.bridge.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.bridge.shard.operator.providers.GlobalConfigurationsProvider;
import com.redhat.service.bridge.shard.operator.providers.TemplateProvider;
import com.redhat.service.bridge.shard.operator.resources.BridgeExecutor;
import com.redhat.service.bridge.shard.operator.resources.KnativeTrigger;
import com.redhat.service.bridge.shard.operator.utils.Constants;
import com.redhat.service.bridge.shard.operator.utils.DeploymentSpecUtils;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

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
    }

    @Override
    public Deployment fetchOrCreateBridgeExecutorDeployment(BridgeExecutor bridgeExecutor, Secret secret) {
        Deployment expected = templateProvider.loadBridgeExecutorDeploymentTemplate(bridgeExecutor);

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
        Service expected = templateProvider.loadBridgeExecutorServiceTemplate(bridgeExecutor);
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
            LOGGER.warn("BridgeExecutor '{}' not deleted", processorDTO);
        }
    }

    @Override
    public void createOrUpdateBridgeExecutorSecret(BridgeExecutor bridgeExecutor, ProcessorDTO processorDTO) {
        Secret expected = templateProvider.loadBridgeExecutorSecretTemplate(bridgeExecutor);
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_BOOTSTRAP_SERVERS_ENV_VAR, Base64.getEncoder().encodeToString(processorDTO.getKafkaConnection().getBootstrapServers().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_CLIENT_ID_ENV_VAR, Base64.getEncoder().encodeToString(processorDTO.getKafkaConnection().getClientId().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_CLIENT_SECRET_ENV_VAR, Base64.getEncoder().encodeToString(processorDTO.getKafkaConnection().getClientSecret().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_SECURITY_PROTOCOL_ENV_VAR, Base64.getEncoder().encodeToString(processorDTO.getKafkaConnection().getSecurityProtocol().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_TOPIC_ENV_VAR, Base64.getEncoder().encodeToString(processorDTO.getKafkaConnection().getTopic().getBytes()));
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
    public KnativeTrigger fetchOrCreateKnativeTrigger(BridgeExecutor bridgeExecutor, Service service) {
        LOGGER.info("trigger");
        KnativeTrigger expected = templateProvider.loadBridgeExecutorTriggerTemplate(bridgeExecutor);
        expected.getSpec().setBroker("ob-" + bridgeExecutor.getSpec().getBridgeId().substring(0, 5));
        expected.getSpec().getSubscriber().getRef().setName(service.getMetadata().getName());
        expected.getSpec().getSubscriber().getRef().setNamespace(service.getMetadata().getNamespace());

        KnativeTrigger existing = kubernetesClient.resources(KnativeTrigger.class)
                //                .inNamespace(bridgeIngress.getMetadata().getNamespace())
                .inNamespace("default")
                .withName(bridgeExecutor.getMetadata().getName().substring(0, 8))
                .get();

        LOGGER.info("bridge executor trigger: " + bridgeExecutor.getMetadata().getName());
        LOGGER.info("exists: " + String.valueOf(existing == null));

        if (existing == null || !expected.getSpec().getBroker().equals(existing.getSpec().getBroker()) || !expected.getSpec().getSubscriber().equals(existing.getSpec().getSubscriber())) {
            LOGGER.info("creating");
            KnativeTrigger trigger = kubernetesClient
                    .resources(KnativeTrigger.class)
                    //                    .inNamespace(bridgeIngress.getMetadata().getNamespace())
                    .inNamespace("default")
                    // Best practice would be to generate a new name for the configmap and replace its reference
                    .withName(bridgeExecutor.getMetadata().getName().substring(0, 8))
                    .createOrReplace(expected);
            try {
                LOGGER.info(new ObjectMapper().writeValueAsString(trigger.getSpec()));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return trigger;
        }

        return existing;
    }
}
