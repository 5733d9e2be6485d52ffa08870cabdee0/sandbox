package com.redhat.service.smartevents.shard.operator.v2.providers;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v2.TestSupport;
import com.redhat.service.smartevents.shard.operator.v2.resources.CamelIntegration;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.CustomResource;

import static org.assertj.core.api.Assertions.assertThat;

public class TemplateProviderImplV2Test {

    private static ObjectMapper objectMapper = new ObjectMapper();

    private static final ManagedProcessor MANAGED_PROCESSOR = ManagedProcessor.fromBuilder()
            .withProcessorName("id")
            .withNamespace("ns")
            .withBridgeId(TestSupport.BRIDGE_ID)
            .withCustomerId(TestSupport.CUSTOMER_ID)
            .withProcessorId("id")
            .withDefinition(objectMapper.createObjectNode())
            .build();

    @Test
    public void metadataIsUpdated() {
        TemplateProviderV2 templateProvider = new TemplateProviderImplV2();
        CamelIntegration camelIntegration = templateProvider.loadCamelIntegrationTemplate(MANAGED_PROCESSOR, getConfig());
        assertOwnerReference(MANAGED_PROCESSOR, camelIntegration.getMetadata());
        assertThat(camelIntegration.getMetadata().getName()).isEqualTo(MANAGED_PROCESSOR.getMetadata().getName());
        assertThat(camelIntegration.getMetadata().getNamespace()).isEqualTo(MANAGED_PROCESSOR.getMetadata().getNamespace());
    }

    @Test
    public void bridgeCamelIntegrationTemplateIsProvided() {
        TemplateProviderV2 templateProvider = new TemplateProviderImplV2();
        CamelIntegration camelIntegration = templateProvider.loadCamelIntegrationTemplate(MANAGED_PROCESSOR, getConfig());

        assertOwnerReference(MANAGED_PROCESSOR, camelIntegration.getMetadata());
        assertLabels(camelIntegration.getMetadata(), MANAGED_PROCESSOR.COMPONENT_NAME);
        assertThat(camelIntegration.getMetadata().getName()).isEqualTo("proc-id");
        assertThat(camelIntegration.getMetadata().getNamespace()).isEqualTo("ns");
        assertThat(camelIntegration.getSpec().getFlows()).isEqualTo(new ArrayList<>());
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

    private TemplateImportConfig getConfig() {
        return TemplateImportConfig.withDefaults(LabelsBuilder.V2_OPERATOR_NAME);
    }
}
