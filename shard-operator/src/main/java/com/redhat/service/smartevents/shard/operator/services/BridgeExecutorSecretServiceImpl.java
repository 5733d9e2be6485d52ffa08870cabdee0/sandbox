package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.KafkaConfiguration;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Base64;
import java.util.List;

@ApplicationScoped
public class BridgeExecutorSecretServiceImpl implements BridgeExecutorSecretService {

    public static final String KAFKA_ERROR_STRATEGY_IGNORE = "ignore";
    public static final String KAFKA_ERROR_STRATEGY_DLQ = "dead-letter-queue";

    @Inject
    TemplateProvider templateProvider;

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public Secret createBridgeExecutorSecret(BridgeExecutor bridgeExecutor) {
        String kafkaErrorStrategy = processorDTO.getType() == ProcessorType.ERROR_HANDLER
                ? KAFKA_ERROR_STRATEGY_IGNORE
                : KAFKA_ERROR_STRATEGY_DLQ;

        Secret expected = templateProvider.loadBridgeExecutorSecretTemplate(bridgeExecutor, TemplateImportConfig.withDefaults());
        KafkaConfiguration kafkaConfiguration = bridgeExecutor.getSpec().getKafkaConfiguration();
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_BOOTSTRAP_SERVERS_ENV_VAR,kafkaConfiguration.getBootstrapServers());
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_CLIENT_ID_ENV_VAR, kafkaConfiguration.getClientId());
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_CLIENT_SECRET_ENV_VAR, kafkaConfiguration.getClientSecret());
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_SECURITY_PROTOCOL_ENV_VAR, kafkaConfiguration.getSecurityProtocol());
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_TOPIC_ENV_VAR, kafkaConfiguration.getTopic());
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_ERROR_STRATEGY_ENV_VAR, kafkaErrorStrategy);
        expected.getData().put(GlobalConfigurationsConstants.KAFKA_ERROR_TOPIC_ENV_VAR, kafkaConfiguration.getErrorTopic());
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
