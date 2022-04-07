package com.redhat.service.bridge.shard.operator;

import java.util.Base64;

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
import com.redhat.service.bridge.shard.operator.resources.istio.AuthorizationPolicy;
import com.redhat.service.bridge.shard.operator.resources.knative.KnativeBroker;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class BridgeIngressServiceImpl implements BridgeIngressService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeIngressServiceImpl.class);

    @ConfigProperty(name = "event-bridge.ingress.image")
    String ingressImage;

    @ConfigProperty(name = "event-bridge.ingress.deployment.timeout-seconds")
    int deploymentTimeout;

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

        BridgeIngress expected = BridgeIngress.fromDTO(bridgeDTO, namespace.getMetadata().getName(), ingressImage);

        BridgeIngress existing = kubernetesClient
                .resources(BridgeIngress.class)
                .inNamespace(namespace.getMetadata().getName())
                .withName(BridgeIngress.resolveResourceName(bridgeDTO.getId()))
                .get();

        if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            BridgeIngress bridgeIngress = kubernetesClient
                    .resources(BridgeIngress.class)
                    .inNamespace(namespace.getMetadata().getName())
                    .createOrReplace(expected);

            // create or update the secrets for the bridgeIngress
            createOrUpdateBridgeIngressSecret(bridgeIngress, bridgeDTO);
        }
    }

    @Override
    public void deleteBridgeIngress(BridgeDTO bridgeDTO) {
        final String namespace = customerNamespaceProvider.resolveName(bridgeDTO.getCustomerId());
        final boolean bridgeDeleted =
                kubernetesClient
                        .resources(BridgeIngress.class)
                        .inNamespace(namespace)
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
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_NAME, Base64.getEncoder().encodeToString(bridgeDTO.getKafkaConnection().getTopic().getBytes()));
        // TODO: refactor with data from DTO
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_SASL_MECHANISM, Base64.getEncoder().encodeToString("PLAIN".getBytes()));

        Secret existing = kubernetesClient
                .secrets()
                .inNamespace(bridgeIngress.getMetadata().getNamespace())
                .withName(bridgeIngress.getMetadata().getName())
                .get();

        if (existing == null || !expected.getData().equals(existing.getData())) {
            kubernetesClient
                    .secrets()
                    .inNamespace(bridgeIngress.getMetadata().getNamespace())
                    .withName(bridgeIngress.getMetadata().getName())
                    .createOrReplace(expected);
        }
    }

    @Override
    public Secret fetchBridgeIngressSecret(BridgeIngress bridgeIngress) {
        return kubernetesClient
                .secrets()
                .inNamespace(bridgeIngress.getMetadata().getNamespace())
                .withName(bridgeIngress.getMetadata().getName())
                .get();
    }

    @Override
    public ConfigMap fetchOrCreateBridgeIngressConfigMap(BridgeIngress bridgeIngress, Secret secret) {
        ConfigMap expected = templateProvider.loadBridgeIngressConfigMapTemplate(bridgeIngress);

        expected.getData().replace("default.topic.partitions", "10");
        expected.getData().replace("default.topic.replication.factor", "3");
        expected.getData().replace("bootstrap.servers", new String(Base64.getDecoder().decode(secret.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_BOOTSTRAP_SERVERS))));
        expected.getData().replace("auth.secret.ref.name", secret.getMetadata().getName());
        expected.getData().replace("topic.name", new String(Base64.getDecoder().decode(secret.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_NAME))));

        ConfigMap existing = kubernetesClient
                .configMaps()
                .inNamespace(bridgeIngress.getMetadata().getNamespace())
                .withName(bridgeIngress.getMetadata().getName())
                .get();

        if (existing == null || !expected.getData().equals(existing.getData())) {
            return kubernetesClient
                    .configMaps()
                    .inNamespace(bridgeIngress.getMetadata().getNamespace())
                    // Best practice would be to generate a new name for the configmap and replace its reference
                    .withName(bridgeIngress.getMetadata().getName())
                    .createOrReplace(expected);
        }

        return existing;
    }

    @Override
    public KnativeBroker fetchOrCreateBridgeIngressBroker(BridgeIngress bridgeIngress, ConfigMap configMap) {
        KnativeBroker expected = templateProvider.loadBridgeIngressBrokerTemplate(bridgeIngress);
        expected.getSpec().getConfig().setName(configMap.getMetadata().getName());
        expected.getSpec().getConfig().setNamespace(configMap.getMetadata().getNamespace());
        expected.getMetadata().getAnnotations().replace("x-kafka.eventing.knative.dev/external.topic", configMap.getData().get("topic.name"));

        KnativeBroker existing = kubernetesClient.resources(KnativeBroker.class)
                .inNamespace(bridgeIngress.getMetadata().getNamespace())
                .withName(bridgeIngress.getMetadata().getName())
                .get();

        if (existing == null || !expected.getSpec().getConfig().equals(existing.getSpec().getConfig())) {
            return kubernetesClient
                    .resources(KnativeBroker.class)
                    .inNamespace(bridgeIngress.getMetadata().getNamespace())
                    // Best practice would be to generate a new name for the configmap and replace its reference
                    .withName(bridgeIngress.getMetadata().getName())
                    .createOrReplace(expected);
        }

        return existing;
    }

    @Override
    public AuthorizationPolicy fetchOrCreateBridgeIngressAuthorizationPolicy(BridgeIngress bridgeIngress) {
        AuthorizationPolicy expected = templateProvider.loadBridgeIngressAuthorizationPolicyTemplate(bridgeIngress);
        expected.getSpec().setAction("ALLOW");
        expected.getSpec().getRules().get(0).getWhen().get(0).getValues().set(0, bridgeIngress.getSpec().getCustomerId());
        expected.getSpec().getRules().get(0).getTo().get(0).getOperation().getPaths().set(0,
                "/" + bridgeIngress.getMetadata().getNamespace() + "/" + bridgeIngress.getMetadata().getName());

        AuthorizationPolicy existing = kubernetesClient.resources(AuthorizationPolicy.class)
                //                .inNamespace(bridgeIngress.getMetadata().getNamespace())
                .inNamespace("istio-system") // https://github.com/istio/istio/issues/37221
                .withName(bridgeIngress.getMetadata().getName())
                .get();

        if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            return kubernetesClient
                    .resources(AuthorizationPolicy.class)
                    //                    .inNamespace(bridgeIngress.getMetadata().getNamespace())
                    .inNamespace("istio-system") // https://github.com/istio/istio/issues/37221
                    .withName(bridgeIngress.getMetadata().getName())
                    .createOrReplace(expected);
        }

        return existing;
    }
}
