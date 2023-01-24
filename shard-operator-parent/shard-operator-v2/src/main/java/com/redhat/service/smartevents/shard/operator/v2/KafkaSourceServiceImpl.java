package com.redhat.service.smartevents.shard.operator.v2;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.v2.providers.TemplateProviderV2;
import com.redhat.service.smartevents.shard.operator.v2.resources.KafkaConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.resources.SourceConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.knative.KafkaSource;
import com.redhat.service.smartevents.shard.operator.v2.resources.knative.KafkaSourceSpec;

import io.fabric8.kubernetes.client.KubernetesClient;

import static com.redhat.service.smartevents.shard.operator.core.providers.GlobalConfigurationsConstants.KAFKA_PASSWORD_SECRET;
import static com.redhat.service.smartevents.shard.operator.core.providers.GlobalConfigurationsConstants.KAFKA_USER_SECRET;
import static com.redhat.service.smartevents.shard.operator.core.providers.GlobalConfigurationsConstants.KNATIVE_KAFKA_SASL_MECHANISM_SECRET;
import static com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder.V2_OPERATOR_NAME;
import static com.redhat.service.smartevents.shard.operator.v2.ManagedBridgeServiceImpl.SOURCE_CONFIGURATION_SECRET_NAME;

@ApplicationScoped
public class KafkaSourceServiceImpl implements KafkaSourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaSourceServiceImpl.class);

    @V2
    @Inject
    TemplateProviderV2 templateProvider;

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public KafkaSource fetchOrCreateKafkaSource(ManagedBridge managedBridge, KnativeBroker knativeBroker) {
        KafkaSource expected = createExpectedKafkaSource(managedBridge, knativeBroker);
        KafkaSource existing = fetchDeployedKafkaSource(managedBridge);
        return processDelta(expected, existing, managedBridge);
    }

    public KafkaSource createExpectedKafkaSource(ManagedBridge managedBridge, KnativeBroker knativeBroker) {
        SourceConfigurationSpec sourceConfiguration = managedBridge.getSpec().getSourceConfigurationSpec();
        if (sourceConfiguration == null) {
            return null;
        }
        KafkaSource expected = templateProvider.loadKafkaSourceTemplate(managedBridge, TemplateImportConfig.withDefaults(V2_OPERATOR_NAME));
        KafkaSourceSpec kafkaSourceSpec = expected.getSpec();
        KafkaConfigurationSpec kafkaConfiguration = sourceConfiguration.getKafkaConfiguration();
        kafkaSourceSpec.setBootstrapServers(List.of(kafkaConfiguration.getBootstrapServers()));
        kafkaSourceSpec.setTopics(List.of(kafkaConfiguration.getTopic()));
        kafkaSourceSpec.getSink().getRef().setName(knativeBroker.getMetadata().getName());
        kafkaSourceSpec.getNet().getSasl().getUser().getSecretKeyRef().setName(SOURCE_CONFIGURATION_SECRET_NAME);
        kafkaSourceSpec.getNet().getSasl().getUser().getSecretKeyRef().setKey(KAFKA_USER_SECRET);
        kafkaSourceSpec.getNet().getSasl().getPassword().getSecretKeyRef().setName(SOURCE_CONFIGURATION_SECRET_NAME);
        kafkaSourceSpec.getNet().getSasl().getPassword().getSecretKeyRef().setKey(KAFKA_PASSWORD_SECRET);
        kafkaSourceSpec.getNet().getSasl().getType().getSecretKeyRef().setName(SOURCE_CONFIGURATION_SECRET_NAME);
        kafkaSourceSpec.getNet().getSasl().getType().getSecretKeyRef().setKey(KNATIVE_KAFKA_SASL_MECHANISM_SECRET);
        return expected;
    }

    public KafkaSource fetchDeployedKafkaSource(ManagedBridge managedBridge) {
        return kubernetesClient.resources(KafkaSource.class)
                .inNamespace(managedBridge.getMetadata().getNamespace())
                .withName(managedBridge.getMetadata().getName())
                .get();
    }

    public KafkaSource processDelta(KafkaSource expected, KafkaSource existing, ManagedBridge managedBridge) {
        if (expected == null) {
            kubernetesClient
                    .resources(KnativeBroker.class)
                    .inNamespace(managedBridge.getMetadata().getNamespace())
                    .withName(managedBridge.getMetadata().getName())
                    .delete();
            return null;
        } else if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            LOGGER.info("Create/Update KnativeSource with name '{}' in namespace '{}' for ManagedBridge with id '{}'", expected.getMetadata().getName(), expected.getMetadata().getNamespace(),
                    managedBridge.getMetadata().getName());
            return kubernetesClient
                    .resources(KafkaSource.class)
                    .inNamespace(managedBridge.getMetadata().getNamespace())
                    .withName(managedBridge.getMetadata().getName())
                    .createOrReplace(expected);
        } else {
            return existing;
        }
    }
}
