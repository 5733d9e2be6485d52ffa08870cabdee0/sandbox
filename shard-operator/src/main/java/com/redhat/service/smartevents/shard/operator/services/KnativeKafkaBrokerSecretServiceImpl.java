package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.KafkaConfiguration;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Base64;

@ApplicationScoped
public class KnativeKafkaBrokerSecretServiceImpl implements KnativeKafkaBrokerSecretService {

    @Inject
    TemplateProvider templateProvider;

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public Secret createKnativeKafkaBrokerSecret(BridgeIngress bridgeIngress) {
        Secret kafkaSecret = templateProvider.loadBridgeIngressSecretTemplate(bridgeIngress, TemplateImportConfig.withDefaults());

        KafkaConfiguration kafkaConfiguration = bridgeIngress.getSpec().getKafkaConfiguration();
        kafkaSecret.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_PROTOCOL_SECRET, Base64.getEncoder().encodeToString(kafkaConfiguration.getSecurityProtocol().getBytes()));
        kafkaSecret.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_SASL_MECHANISM_SECRET, Base64.getEncoder().encodeToString(kafkaConfiguration.getSaslMechanism().getBytes()));
        kafkaSecret.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_USER_SECRET, Base64.getEncoder().encodeToString(kafkaConfiguration.getClientId().getBytes()));
        kafkaSecret.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_PASSWORD_SECRET, Base64.getEncoder().encodeToString(kafkaConfiguration.getClientSecret().getBytes()));
        return kafkaSecret;
    }

    @Override
    public Secret fetchKnativeKafkaBrokerSecret(BridgeIngress bridgeIngress) {
        return kubernetesClient
                .secrets()
                .inNamespace(bridgeIngress.getMetadata().getNamespace())
                .withName(bridgeIngress.getMetadata().getName())
                .get();
    }
}
