package com.redhat.service.smartevents.shard.operator.v2;

import java.util.Base64;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.core.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.v2.providers.NamespaceProvider;
import com.redhat.service.smartevents.shard.operator.v2.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.v2.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.v2.resources.KafkaConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;

import com.redhat.service.smartevents.shard.operator.v2.resources.TLSSpec;
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

        ManagedBridge expected = ManagedBridge.fromDTO(bridgeDTO, expectedNamespace);
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

            createOrUpdateBridgeSecret(expected);
        }

        /*
            Send callback to Control Plane for initial creation with initial conditions
         */


    }

    private void createOrUpdateBridgeSecret(ManagedBridge managedBridge) {
        Secret expected = templateProvider.loadManagedBridgeSecretTemplate(managedBridge, TemplateImportConfig.withDefaults());

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

        ManagedBridge mb = ManagedBridge.fromDTO(bridgeDTO, null);

        /*
         * Pull in the rest of the logic from BridgeIngressServiceImpl to delete the other resources
         */

        namespaceProvider.deleteNamespace(mb);
    }
}
