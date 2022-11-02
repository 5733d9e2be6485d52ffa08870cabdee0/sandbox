package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class KnativeKafkaBrokerConfigMapServiceImpl implements KnativeKafkaBrokerConfigMapService {

    @Inject
    TemplateProvider templateProvider;

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public ConfigMap createKnativeKafkaBrokerConfigMap(BridgeIngress bridgeIngress) {
        ConfigMap expected = templateProvider.loadBridgeIngressConfigMapTemplate(bridgeIngress, TemplateImportConfig.withDefaults());

        expected.getData().replace(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_PARTITIONS_CONFIGMAP, GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_PARTITIONS_VALUE_CONFIGMAP); // TODO: move to DTO?
        expected.getData().replace(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_REPLICATION_FACTOR_CONFIGMAP, GlobalConfigurationsConstants.KNATIVE_KAFKA_REPLICATION_FACTOR_VALUE_CONFIGMAP); // TODO: move to DTO?
        expected.getData().replace(GlobalConfigurationsConstants.KNATIVE_KAFKA_BOOTSTRAP_SERVERS_CONFIGMAP, bridgeIngress.getSpec().getKafkaConfiguration().getBootstrapServers());
        expected.getData().replace(GlobalConfigurationsConstants.KNATIVE_KAFKA_AUTH_SECRET_REF_NAME_CONFIGMAP, bridgeIngress.getMetadata().getName());
        expected.getData().replace(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_NAME_CONFIGMAP, bridgeIngress.getSpec().getKafkaConfiguration().getTopic());
        return expected;
    }

    @Override
    public ConfigMap fetchKnativeKafkaBrokerConfigMap(BridgeIngress bridgeIngress) {
        return kubernetesClient
                .configMaps()
                .inNamespace(bridgeIngress.getMetadata().getNamespace())
                .withName(bridgeIngress.getMetadata().getName())
                .get();
    }
}
