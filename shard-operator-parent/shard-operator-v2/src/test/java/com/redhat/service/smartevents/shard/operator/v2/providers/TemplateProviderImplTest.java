package com.redhat.service.smartevents.shard.operator.v2.providers;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v2.Fixtures;
import com.redhat.service.smartevents.shard.operator.v2.resources.DNSConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.KNativeBrokerConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.KafkaConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.resources.TLSSpec;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.CustomResource;

import static org.assertj.core.api.Assertions.assertThat;

public class TemplateProviderImplTest {

    private static final DNSConfigurationSpec DNS_CONFIGURATION = DNSConfigurationSpec.Builder.builder()
            .host(Fixtures.BRIDGE_ENDPOINT)
            .tls(new TLSSpec(Fixtures.BRIDGE_TLS_CERTIFICATE, Fixtures.BRIDGE_TLS_KEY))
            .build();

    private static final KafkaConfigurationSpec KAFKA_CONFIGURATION_SPEC = KafkaConfigurationSpec.Builder.builder()
            .topic(Fixtures.KAFKA_TOPIC)
            .securityProtocol(Fixtures.KAFKA_SECURITY_PROTOCOL)
            .user(Fixtures.KAFKA_CLIENT_ID)
            .password(Fixtures.KAFKA_CLIENT_SECRET)
            .saslMechanism(Fixtures.KAFKA_SASL_MECHANISM)
            .build();

    private static final ManagedBridge MANAGED_BRIDGE = ManagedBridge.fromBuilder()
            .withBridgeName("id")
            .withNamespace("ns")
            .withBridgeId("12345")
            .withCustomerId("12456")
            .withDnsConfigurationSpec(DNS_CONFIGURATION)
            .withKnativeBrokerConfigurationSpec(new KNativeBrokerConfigurationSpec(KAFKA_CONFIGURATION_SPEC))
            .withOwner("foo")
            .build();

    @Test
    public void managedBridgeSecretTemplateIsProvided() {
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Secret secret = templateProvider.loadManagedBridgeSecretTemplate(MANAGED_BRIDGE, TemplateImportConfig.withDefaults());

        assertOwnerReference(MANAGED_BRIDGE, secret.getMetadata());
        assertLabels(secret.getMetadata(), ManagedBridge.COMPONENT_NAME);

        assertThat(secret.getData().get("protocol")).isBlank();
        assertThat(secret.getData().get("sasl.mechanism")).isBlank();
        assertThat(secret.getData().get("user")).isBlank();
        assertThat(secret.getData().get("password")).isBlank();
        assertThat(secret.getData().get("bootstrap.servers")).isBlank();
        assertThat(secret.getData().get("topic.name")).isBlank();
    }

    private void assertLabels(ObjectMeta meta, String component) {
        assertThat(meta.getLabels().get(LabelsBuilder.COMPONENT_LABEL)).isEqualTo(component);
        assertThat(meta.getLabels().get(LabelsBuilder.MANAGED_BY_LABEL)).isEqualTo(LabelsBuilder.V2_OPERATOR_NAME);
        assertThat(meta.getLabels().get(LabelsBuilder.CREATED_BY_LABEL)).isEqualTo(LabelsBuilder.V2_OPERATOR_NAME);
    }

    private void assertOwnerReference(CustomResource resource, ObjectMeta meta) {
        assertThat(meta.getOwnerReferences().size()).isEqualTo(1);

        OwnerReference ownerReference = meta.getOwnerReferences().get(0);
        assertThat(ownerReference.getName()).isEqualTo(resource.getMetadata().getName());
        assertThat(ownerReference.getApiVersion()).isEqualTo(resource.getApiVersion());
        assertThat(ownerReference.getKind()).isEqualTo(resource.getKind());
        assertThat(ownerReference.getUid()).isEqualTo(resource.getMetadata().getUid());
    }

}
