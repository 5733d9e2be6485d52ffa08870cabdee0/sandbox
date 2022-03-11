package com.redhat.service.bridge.shard.operator;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.bridge.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.bridge.shard.operator.providers.GlobalConfigurationsProvider;
import com.redhat.service.bridge.shard.operator.providers.TemplateProvider;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.resources.KnativeBroker;
import com.redhat.service.bridge.shard.operator.utils.Constants;
import com.redhat.service.bridge.shard.operator.utils.DeploymentSpecUtils;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class BridgeIngressServiceImpl implements BridgeIngressService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeIngressServiceImpl.class);

    @ConfigProperty(name = "event-bridge.ingress.image")
    String ingressImage;

    @ConfigProperty(name = "event-bridge.ingress.deployment.timeout-seconds")
    int deploymentTimeout;

    @ConfigProperty(name = "event-bridge.webhook.technical-account-id")
    String webhookTechnicalAccountId;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @Inject
    TemplateProvider templateProvider;

    @Inject
    GlobalConfigurationsProvider globalConfigurationsProvider;

    @Override
    public void createBridgeIngress(BridgeDTO bridgeDTO) {
        final Namespace namespace = customerNamespaceProvider.fetchOrCreateCustomerNamespace(bridgeDTO.getCustomerId());

        BridgeIngress expected = BridgeIngress.fromDTO(bridgeDTO, "default", ingressImage);

        BridgeIngress existing = kubernetesClient
                .resources(BridgeIngress.class)
                .inNamespace("default")
                //                .inNamespace(namespace.getMetadata().getName())
                .withName(BridgeIngress.resolveResourceName(bridgeDTO.getId()))
                .get();

        if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            BridgeIngress bridgeIngress = kubernetesClient
                    .resources(BridgeIngress.class)
                    .inNamespace("default")
                    //                    .inNamespace(namespace.getMetadata().getName())
                    .createOrReplace(expected);

            // create or update the secrets for the bridgeIngress
            createOrUpdateBridgeIngressSecret(bridgeIngress, bridgeDTO);
        }
    }

    // TODO: remove
    @Override
    public Deployment fetchOrCreateBridgeIngressDeployment(BridgeIngress bridgeIngress, Secret secret) {
        Deployment expected = templateProvider.loadBridgeIngressDeploymentTemplate(bridgeIngress);

        // Specs
        expected.getSpec().getSelector().setMatchLabels(new LabelsBuilder().withAppInstance(bridgeIngress.getMetadata().getName()).build());
        expected.getSpec().getTemplate().getMetadata().setLabels(new LabelsBuilder().withAppInstance(bridgeIngress.getMetadata().getName()).build());
        expected.getSpec().getTemplate().getSpec().getContainers().get(0).setName(BridgeIngress.COMPONENT_NAME);
        expected.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(bridgeIngress.getSpec().getImage());
        expected.getSpec().setProgressDeadlineSeconds(deploymentTimeout);

        List<EnvVar> environmentVariables = new ArrayList<>();
        environmentVariables.add(new EnvVarBuilder().withName(GlobalConfigurationsConstants.SSO_URL_CONFIG_ENV_VAR).withValue(globalConfigurationsProvider.getSsoUrl()).build());
        environmentVariables.add(new EnvVarBuilder().withName(GlobalConfigurationsConstants.SSO_CLIENT_ID_CONFIG_ENV_VAR).withValue(globalConfigurationsProvider.getSsoClientId()).build());
        environmentVariables.add(new EnvVarBuilder().withName(Constants.BRIDGE_INGRESS_CUSTOMER_ID_CONFIG_ENV_VAR).withValue(bridgeIngress.getSpec().getCustomerId()).build());
        environmentVariables.add(new EnvVarBuilder().withName(Constants.BRIDGE_INGRESS_WEBHOOK_TECHNICAL_ACCOUNT_ID).withValue(webhookTechnicalAccountId).build());

        expected.getSpec().getTemplate().getSpec().getContainers().get(0).setEnv(environmentVariables);
        expected.getSpec().getTemplate().getSpec().getContainers().get(0).getEnvFrom().get(0).getSecretRef().setName(secret.getMetadata().getName());

        Deployment existing = kubernetesClient.apps().deployments().inNamespace(bridgeIngress.getMetadata().getNamespace()).withName(bridgeIngress.getMetadata().getName()).get();

        if (existing == null || !DeploymentSpecUtils.isDeploymentEqual(expected, existing)) {
            return kubernetesClient.apps().deployments().inNamespace(bridgeIngress.getMetadata().getNamespace()).createOrReplace(expected);
        }

        return existing;
    }

    // TODO: remove
    @Override
    public Service fetchOrCreateBridgeIngressService(BridgeIngress bridgeIngress, Deployment deployment) {
        Service expected = templateProvider.loadBridgeIngressServiceTemplate(bridgeIngress);

        // Specs
        expected.getSpec().setSelector(new LabelsBuilder().withAppInstance(deployment.getMetadata().getName()).build());
        // The service must have a label to link with a supposed ServiceMonitor: https://prometheus-operator.dev/docs/operator/troubleshooting/#overview-of-servicemonitor-tagging-and-related-elements
        if (expected.getMetadata().getLabels() == null) {
            expected.getMetadata().setLabels(new HashMap<>());
        }
        expected.getMetadata().getLabels().putAll(new LabelsBuilder().withAppInstance(deployment.getMetadata().getName()).buildWithDefaults());

        Service existing = kubernetesClient.services().inNamespace(bridgeIngress.getMetadata().getNamespace()).withName(bridgeIngress.getMetadata().getName()).get();

        if (existing == null
                || !expected.getSpec().getSelector().equals(existing.getSpec().getSelector())
                || !expected.getMetadata().getLabels().equals(existing.getMetadata().getLabels())) {
            return kubernetesClient.services().inNamespace(bridgeIngress.getMetadata().getNamespace()).createOrReplace(expected);
        }

        return existing;
    }

    @Override
    public void deleteBridgeIngress(BridgeDTO bridgeDTO) {
        final String namespace = customerNamespaceProvider.resolveName(bridgeDTO.getCustomerId());
        final boolean bridgeDeleted =
                kubernetesClient
                        .resources(BridgeIngress.class)
                        .inNamespace("default")
                        //                        .inNamespace(namespace)
                        .delete(BridgeIngress.fromDTO(bridgeDTO, namespace, ingressImage));
        if (!bridgeDeleted) {
            // TODO: we might need to review this use case and have a manager to look at a queue of objects not deleted and investigate. Unfortunately the API does not give us a reason.
            LOGGER.warn("BridgeIngress '{}' not deleted", bridgeDTO);
        }
    }

    @Override
    public void createOrUpdateBridgeIngressSecret(BridgeIngress bridgeIngress, BridgeDTO bridgeDTO) {
        Secret expected = templateProvider.loadBridgeIngressSecretTemplate(bridgeIngress);
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_BOOTSTRAP_SERVERS, Base64.getEncoder().encodeToString(bridgeDTO.getKafkaConnection().getBootstrapServers().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_USER, Base64.getEncoder().encodeToString(bridgeDTO.getKafkaConnection().getClientId().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_PASSWORD, Base64.getEncoder().encodeToString(bridgeDTO.getKafkaConnection().getClientSecret().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_PROTOCOL, Base64.getEncoder().encodeToString(bridgeDTO.getKafkaConnection().getSecurityProtocol().getBytes()));
        // TODO: refactor with data from DTO
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_SASL_MECHANISM, Base64.getEncoder().encodeToString("PLAIN".getBytes()));

        Secret existing = kubernetesClient
                .secrets()
                .inNamespace("default")
                //                .inNamespace(bridgeIngress.getMetadata().getNamespace().substring(0, 8))
                .withName(bridgeIngress.getMetadata().getName())
                .get();

        if (existing == null || !expected.getData().equals(existing.getData())) {
            kubernetesClient
                    .secrets()
                    .inNamespace("default")
                    //                    .inNamespace(bridgeIngress.getMetadata().getNamespace())
                    .withName(bridgeIngress.getMetadata().getName().substring(0, 8))
                    .createOrReplace(expected);
        }
    }

    @Override
    public Secret fetchBridgeIngressSecret(BridgeIngress bridgeIngress) {
        return kubernetesClient
                .secrets()
                .inNamespace("default")
                //                .inNamespace(bridgeIngress.getMetadata().getNamespace())
                .withName(bridgeIngress.getMetadata().getName().substring(0, 8))
                .get();
    }

    @Override
    public ConfigMap fetchOrCreateBridgeIngressConfigMap(BridgeIngress bridgeIngress, Secret secret) {
        ConfigMap expected = templateProvider.loadBridgeIngressConfigMapTemplate(bridgeIngress);

        expected.getData().replace("default.topic.partitions", "10");
        expected.getData().replace("default.topic.replication.factor", "3");
        expected.getData().replace("bootstrap.servers", new String(Base64.getDecoder().decode(secret.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_BOOTSTRAP_SERVERS))));
        expected.getData().replace("auth.secret.ref.name", secret.getMetadata().getName());

        ConfigMap existing = kubernetesClient
                .configMaps()
                //                .inNamespace(bridgeIngress.getMetadata().getNamespace())
                .inNamespace("default")
                .withName(bridgeIngress.getMetadata().getName().substring(0, 8))
                .get();

        if (existing == null || !expected.getData().equals(existing.getData())) {
            return kubernetesClient
                    .configMaps()
                    //                    .inNamespace(bridgeIngress.getMetadata().getNamespace())
                    .inNamespace("default")
                    // Best practice would be to generate a new name for the configmap and replace its reference
                    .withName(bridgeIngress.getMetadata().getName().substring(0, 8))
                    .createOrReplace(expected);
        }

        return existing;
    }

    @Override
    public KnativeBroker fetchOrCreateBridgeIngressBroker(BridgeIngress bridgeIngress, ConfigMap configMap) {
        KnativeBroker expected = templateProvider.loadBridgeIngressBrokerTemplate(bridgeIngress);
        expected.getSpec().getConfig().setName(configMap.getMetadata().getName());
        expected.getSpec().getConfig().setNamespace(configMap.getMetadata().getNamespace());

        KnativeBroker existing = kubernetesClient.resources(KnativeBroker.class)
                //                .inNamespace(bridgeIngress.getMetadata().getNamespace())
                .inNamespace("default")
                .withName(bridgeIngress.getMetadata().getName().substring(0, 8))
                .get();

        if (existing == null || !expected.getSpec().getConfig().equals(existing.getSpec().getConfig())) {
            return kubernetesClient
                    .resources(KnativeBroker.class)
                    //                    .inNamespace(bridgeIngress.getMetadata().getNamespace())
                    .inNamespace("default")
                    // Best practice would be to generate a new name for the configmap and replace its reference
                    .withName(bridgeIngress.getMetadata().getName().substring(0, 8))
                    .createOrReplace(expected);
        }

        return existing;
    }
}
