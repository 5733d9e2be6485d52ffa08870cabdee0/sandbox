package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.KafkaConfiguration;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BridgeExecutorSecretServiceImpl implements BridgeExecutorSecretService {

    @Inject
    TemplateProvider templateProvider;

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public Secret createBridgeExecutorSecret(BridgeExecutor bridgeExecutor) {
        Secret expected = templateProvider.loadBridgeExecutorSecretTemplate(bridgeExecutor, TemplateImportConfig.withDefaults());
        KafkaConfiguration kafkaConfiguration = bridgeExecutor.getSpec().getKafkaConfiguration();
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_BOOTSTRAP_SERVERS_ENV_VAR,kafkaConfiguration.getBootstrapServers());
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_CLIENT_ID_ENV_VAR, kafkaConfiguration.getClientId());
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_CLIENT_SECRET_ENV_VAR, kafkaConfiguration.getClientSecret());
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_SECURITY_PROTOCOL_ENV_VAR, kafkaConfiguration.getSecurityProtocol());
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_TOPIC_ENV_VAR, kafkaConfiguration.getTopic());
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_GROUP_ID_ENV_VAR, bridgeExecutor.getSpec().getId());

        return expected;
    }

    @Override
    public Secret fetchBridgeExecutorSecret(BridgeExecutor bridgeExecutor) {
        return kubernetesClient
                .secrets()
                .inNamespace(bridgeExecutor.getMetadata().getNamespace())
                .withName(bridgeExecutor.getMetadata().getName())
                .get();
    }
}
