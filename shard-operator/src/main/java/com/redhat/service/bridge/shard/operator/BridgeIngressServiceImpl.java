package com.redhat.service.bridge.shard.operator;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.bridge.shard.operator.providers.KafkaConfigurationCostants;
import com.redhat.service.bridge.shard.operator.providers.KafkaConfigurationProvider;
import com.redhat.service.bridge.shard.operator.providers.TemplateProvider;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.utils.Constants;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class BridgeIngressServiceImpl implements BridgeIngressService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeIngressServiceImpl.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @Inject
    TemplateProvider templateProvider;

    @Inject
    KafkaConfigurationProvider kafkaConfigurationProvider;

    @ConfigProperty(name = "event-bridge.ingress.image")
    String ingressImage;

    @Override
    public void createBridgeIngress(BridgeDTO bridgeDTO) {
        final Namespace namespace = customerNamespaceProvider.fetchOrCreateCustomerNamespace(bridgeDTO.getCustomerId());

        kubernetesClient
                .resources(BridgeIngress.class)
                .inNamespace(namespace.getMetadata().getName())
                .create(BridgeIngress.fromDTO(bridgeDTO, namespace.getMetadata().getName(), ingressImage));
    }

    // TODO: if the retrieved resource spec is not equal to the expected one, we should redeploy https://issues.redhat.com/browse/MGDOBR-140
    @Override
    public Deployment fetchOrCreateBridgeIngressDeployment(BridgeIngress bridgeIngress) {
        Deployment deployment = kubernetesClient.apps().deployments().inNamespace(bridgeIngress.getMetadata().getNamespace()).withName(bridgeIngress.getMetadata().getName()).get();

        if (deployment != null) {
            return deployment;
        }

        deployment = templateProvider.loadBridgeDeploymentTemplate(bridgeIngress);

        // Specs
        deployment.getSpec().getSelector().setMatchLabels(new LabelsBuilder().withAppInstance(bridgeIngress.getMetadata().getName()).build());
        deployment.getSpec().getTemplate().getMetadata().setLabels(new LabelsBuilder().withAppInstance(bridgeIngress.getMetadata().getName()).build());
        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setName(LabelsBuilder.BRIDGE_INGRESS_COMPONENT);
        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(bridgeIngress.getSpec().getImage());

        // TODO: All the Ingress applications will push events to the same kafka cluster under the same kafka topic. This configuration will have to be specified by the manager for each Bridge instance: https://issues.redhat.com/browse/MGDOBR-123
        List<EnvVar> environmentVariables = new ArrayList<>();
        environmentVariables.add(new EnvVarBuilder().withName(KafkaConfigurationCostants.KAFKA_BOOTSTRAP_SERVERS_ENV_VAR).withValue(kafkaConfigurationProvider.getBootstrapServers()).build());
        environmentVariables.add(new EnvVarBuilder().withName(KafkaConfigurationCostants.KAFKA_CLIENT_ID_ENV_VAR).withValue(kafkaConfigurationProvider.getClient()).build());
        environmentVariables.add(new EnvVarBuilder().withName(KafkaConfigurationCostants.KAFKA_CLIENT_SECRET_ENV_VAR).withValue(kafkaConfigurationProvider.getSecret()).build());
        environmentVariables.add(new EnvVarBuilder().withName(Constants.BRIDGE_INGRESS_BRIDGE_ID_CONFIG).withValue(bridgeIngress.getSpec().getId()).build());
        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setEnv(environmentVariables);

        return kubernetesClient.apps().deployments().inNamespace(bridgeIngress.getMetadata().getNamespace()).create(deployment);
    }

    // TODO: if the retrieved resource spec is not equal to the expected one, we should redeploy https://issues.redhat.com/browse/MGDOBR-140
    @Override
    public Service fetchOrCreateBridgeIngressService(BridgeIngress bridgeIngress, Deployment deployment) {
        Service service = kubernetesClient.services().inNamespace(bridgeIngress.getMetadata().getNamespace()).withName(bridgeIngress.getMetadata().getName()).get();

        if (service != null) {
            return service;
        }

        service = templateProvider.loadBridgeServiceTemplate(bridgeIngress);

        // Specs
        service.getSpec().setSelector(new LabelsBuilder().withAppInstance(deployment.getMetadata().getName()).build());

        return kubernetesClient.services().inNamespace(bridgeIngress.getMetadata().getNamespace()).create(service);
    }

    @Override
    public void deleteBridgeIngress(BridgeDTO bridgeDTO) {
        final String namespace = customerNamespaceProvider.resolveName(bridgeDTO.getCustomerId());
        final boolean bridgeDeleted =
                kubernetesClient
                        .resources(BridgeIngress.class)
                        .inNamespace(namespace)
                        .delete(BridgeIngress.fromDTO(bridgeDTO, namespace, ingressImage));
        if (bridgeDeleted) {
            customerNamespaceProvider.deleteCustomerNamespaceIfEmpty(bridgeDTO.getCustomerId());
        } else {
            // TODO: we might need to review this use case and have a manager to look at a queue of objects not deleted and investigate. Unfortunately the API does not give us a reason.
            LOGGER.warn("BridgeIngress '{}' not deleted", bridgeDTO);
        }
    }
}
