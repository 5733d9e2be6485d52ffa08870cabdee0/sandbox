package com.redhat.service.smartevents.shard.operator.v2;

import java.util.Base64;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.core.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v2.converters.ManagedBridgeConverter;
import com.redhat.service.smartevents.shard.operator.v2.providers.NamespaceProvider;
import com.redhat.service.smartevents.shard.operator.v2.resources.KafkaConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.resources.TLSSpec;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class ManagedBridgeServiceImpl implements ManagedBridgeService {

    @Inject
    NamespaceProvider namespaceProvider;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TemplateProvider templateProvider;

    @Override
    public void createManagedBridge(BridgeDTO bridgeDTO) {

        String expectedNamespace = namespaceProvider.getNamespaceName(bridgeDTO.getId());

        ManagedBridge expected = ManagedBridgeConverter.fromBridgeDTOToManageBridge(bridgeDTO, expectedNamespace);
        namespaceProvider.fetchOrCreateNamespace(expected);

        ManagedBridge existing = kubernetesClient
                .resources(ManagedBridge.class)
                .inNamespace(expected.getMetadata().getNamespace())
                .withName(expected.getMetadata().getName())
                .get();

        if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            expected = kubernetesClient
                    .resources(ManagedBridge.class)
                    .inNamespace(expected.getMetadata().getNamespace())
                    .withName(expected.getMetadata().getName())
                    .createOrReplace(expected);

            //TODO - Any reason to not move this to the controller?
            createOrUpdateBridgeSecret(expected);
        }

        /*
         * Send callback to Control Plane for initial creation with initial conditions
         */

    }

    private void createOrUpdateBridgeSecret(ManagedBridge managedBridge) {
        Secret expected = templateProvider.loadBridgeIngressSecretTemplate(managedBridge, TemplateImportConfig.withDefaults(LabelsBuilder.V2_OPERATOR_NAME));

        KafkaConfigurationSpec kafkaConfiguration = managedBridge.getSpec().getkNativeBrokerConfiguration().getKafkaConfiguration();

        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_BOOTSTRAP_SERVERS_SECRET,
                Base64.getEncoder().encodeToString(managedBridge.getSpec().getkNativeBrokerConfiguration().getKafkaConfiguration().getBootstrapServers().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_USER_SECRET, Base64.getEncoder().encodeToString(kafkaConfiguration.getUser().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_PASSWORD_SECRET, Base64.getEncoder().encodeToString(kafkaConfiguration.getPassword().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_PROTOCOL_SECRET, Base64.getEncoder().encodeToString(kafkaConfiguration.getSecurityProtocol().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_NAME_SECRET, Base64.getEncoder().encodeToString(kafkaConfiguration.getTopic().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_SASL_MECHANISM_SECRET, Base64.getEncoder().encodeToString(kafkaConfiguration.getSaslMechanism().getBytes()));

        TLSSpec tlsSpec = managedBridge.getSpec().getDnsConfiguration().getTls();

        expected.getData().put(GlobalConfigurationsConstants.TLS_CERTIFICATE_SECRET, Base64.getEncoder().encodeToString(tlsSpec.getCertificate().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.TLS_KEY_SECRET, Base64.getEncoder().encodeToString(tlsSpec.getKey().getBytes()));

        Secret existing = kubernetesClient
                .secrets()
                .inNamespace(managedBridge.getMetadata().getNamespace())
                .withName(managedBridge.getMetadata().getName())
                .get();

        if (existing == null || !expected.getData().equals(existing.getData())) {
            kubernetesClient
                    .secrets()
                    .inNamespace(managedBridge.getMetadata().getNamespace())
                    .withName(managedBridge.getMetadata().getName())
                    .createOrReplace(expected);
        }
    }

    @Override
    public void deleteManagedBridge(BridgeDTO bridgeDTO) {

        ManagedBridge mb = ManagedBridgeConverter.fromBridgeDTOToManageBridge(bridgeDTO, null);

        /*
         * Pull in the rest of the logic from BridgeIngressServiceImpl to delete the other resources
         */

        namespaceProvider.deleteNamespace(mb);
    }

    @Override
    public Secret fetchBridgeSecret(ManagedBridge managedBridge) {
        return kubernetesClient
                .secrets()
                .inNamespace(managedBridge.getMetadata().getNamespace())
                .withName(managedBridge.getMetadata().getName())
                .get();
    }

    @Override
    public ConfigMap fetchOrCreateBridgeConfigMap(ManagedBridge managedBridge, Secret secret) {
        ConfigMap expected = templateProvider.loadBridgeIngressConfigMapTemplate(managedBridge, TemplateImportConfig.withDefaults(LabelsBuilder.V2_OPERATOR_NAME));

        expected.getData().replace(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_PARTITIONS_CONFIGMAP, GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_PARTITIONS_VALUE_CONFIGMAP); // TODO: move to DTO?
        expected.getData().replace(GlobalConfigurationsConstants.KNATIVE_KAFKA_REPLICATION_FACTOR_CONFIGMAP, GlobalConfigurationsConstants.KNATIVE_KAFKA_REPLICATION_FACTOR_VALUE_CONFIGMAP); // TODO: move to DTO?
        expected.getData().replace(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_BOOTSTRAP_SERVERS_CONFIGMAP,
                new String(Base64.getDecoder().decode(secret.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_BOOTSTRAP_SERVERS_SECRET))));
        expected.getData().replace(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_SECRET_REF_NAME_CONFIGMAP, secret.getMetadata().getName());
        expected.getData().replace(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_TOPIC_NAME_CONFIGMAP,
                new String(Base64.getDecoder().decode(secret.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_NAME_SECRET))));

        ConfigMap existing = kubernetesClient
                .configMaps()
                .inNamespace(managedBridge.getMetadata().getNamespace())
                .withName(managedBridge.getMetadata().getName())
                .get();

        if (existing == null || !expected.getData().equals(existing.getData())) {
            return kubernetesClient
                    .configMaps()
                    .inNamespace(managedBridge.getMetadata().getNamespace())
                    // Best practice would be to generate a new name for the configmap and replace its reference
                    .withName(managedBridge.getMetadata().getName())
                    .createOrReplace(expected);
        }

        return existing;
    }

    @Override
    public KnativeBroker fetchOrCreateKnativeBroker(ManagedBridge managedBridge, ConfigMap configMap) {

        KnativeBroker expected = templateProvider.loadBridgeIngressBrokerTemplate(managedBridge, TemplateImportConfig.withDefaults(LabelsBuilder.V2_OPERATOR_NAME));
        expected.getSpec().getConfig().setName(configMap.getMetadata().getName());
        expected.getSpec().getConfig().setNamespace(configMap.getMetadata().getNamespace());
        expected.getMetadata().getAnnotations().replace(GlobalConfigurationsConstants.KNATIVE_BROKER_EXTERNAL_TOPIC_ANNOTATION_NAME,
                configMap.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_TOPIC_NAME_CONFIGMAP));

        KnativeBroker existing = kubernetesClient.resources(KnativeBroker.class)
                .inNamespace(managedBridge.getMetadata().getNamespace())
                .withName(managedBridge.getMetadata().getName())
                .get();

        if (existing == null) {
            return kubernetesClient
                    .resources(KnativeBroker.class)
                    .inNamespace(managedBridge.getMetadata().getNamespace())
                    .withName(managedBridge.getMetadata().getName())
                    .create(expected);
        }

        if (!expected.getSpec().getConfig().equals(existing.getSpec().getConfig())) {
            // knative broker is immutable. We have to delete the resource -> trigger the reconciler -> recreate.
            kubernetesClient
                    .resources(KnativeBroker.class)
                    .inNamespace(managedBridge.getMetadata().getNamespace())
                    .withName(managedBridge.getMetadata().getName())
                    .delete();
            return null;
        }

        return existing;
    }
}
