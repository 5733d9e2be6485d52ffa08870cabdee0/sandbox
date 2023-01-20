package com.redhat.service.smartevents.shard.operator.v1.providers;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redhat.service.smartevents.shard.operator.core.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TemplateProviderV1ImplTest {

    private static final String RESOURCE_NAME = "my-test-name";

    private static final String KIND = "ManagedBridge";

    private static final String API_VERSION = "v2alpha1";

    private static final String NAMESPACE = "my-bridge-namespace";

    private static final String BRIDGE_COMPONENT_NAME = "ingress";

    private static final String EXECUTOR_COMPONENT_NAME = "executor";

    @Mock
    private HasMetadata hasMetadata;

    @Mock
    private ObjectMeta objectMeta;

    private void mockResourceForNoOwnerReference() {
        when(hasMetadata.getMetadata()).thenReturn(objectMeta);
        when(objectMeta.getName()).thenReturn(RESOURCE_NAME);
        when(objectMeta.getNamespace()).thenReturn(NAMESPACE);
    }

    private void mockResourceForOwnerReference() {
        when(hasMetadata.getApiVersion()).thenReturn(API_VERSION);
        when(hasMetadata.getKind()).thenReturn(KIND);

        when(hasMetadata.getMetadata()).thenReturn(objectMeta);
        when(objectMeta.getName()).thenReturn(RESOURCE_NAME);
        when(objectMeta.getNamespace()).thenReturn(NAMESPACE);
        when(objectMeta.getUid()).thenReturn(UUID.randomUUID().toString());
    }

    @Test
    public void metadataIsUpdated() {
        mockResourceForOwnerReference();

        TemplateProviderV1 templateProvider = new TemplateProviderV1Impl();
        Deployment deployment = templateProvider.loadBridgeExecutorDeploymentTemplate(hasMetadata, TemplateImportConfig.withDefaults(LabelsBuilder.V1_OPERATOR_NAME));
        assertOwnerReference(hasMetadata, deployment.getMetadata());
        assertThat(deployment.getMetadata().getName()).isEqualTo(hasMetadata.getMetadata().getName());
        assertThat(deployment.getMetadata().getNamespace()).isEqualTo(hasMetadata.getMetadata().getNamespace());

        deployment = templateProvider.loadBridgeExecutorDeploymentTemplate(hasMetadata, new TemplateImportConfig(LabelsBuilder.V1_OPERATOR_NAME).withNameFromParent());
        assertThat(deployment.getMetadata().getOwnerReferences()).isNull();
        assertThat(deployment.getMetadata().getName()).isEqualTo(hasMetadata.getMetadata().getName());
        assertThat(deployment.getMetadata().getNamespace()).isNull();

        deployment = templateProvider.loadBridgeExecutorDeploymentTemplate(hasMetadata, new TemplateImportConfig(LabelsBuilder.V1_OPERATOR_NAME).withNamespaceFromParent());
        assertThat(deployment.getMetadata().getOwnerReferences()).isNull();
        assertThat(deployment.getMetadata().getName()).isNull();
        assertThat(deployment.getMetadata().getNamespace()).isEqualTo(hasMetadata.getMetadata().getNamespace());

        deployment = templateProvider.loadBridgeExecutorDeploymentTemplate(hasMetadata, new TemplateImportConfig(LabelsBuilder.V1_OPERATOR_NAME).withOwnerReferencesFromParent());
        assertOwnerReference(hasMetadata, deployment.getMetadata());
        assertThat(deployment.getMetadata().getName()).isNull();
        assertThat(deployment.getMetadata().getNamespace()).isNull();
    }

    @Test
    public void bridgeExecutorDeploymentTemplateIsProvided() {
        mockResourceForOwnerReference();
        TemplateProviderV1 templateProvider = new TemplateProviderV1Impl();
        Deployment deployment = templateProvider.loadBridgeExecutorDeploymentTemplate(hasMetadata, TemplateImportConfig.withDefaults(LabelsBuilder.V1_OPERATOR_NAME));

        assertOwnerReference(hasMetadata, deployment.getMetadata());
        assertLabels(deployment.getMetadata(), EXECUTOR_COMPONENT_NAME);
        assertThat(deployment.getSpec().getReplicas()).isEqualTo(1);
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getLivenessProbe()).isNotNull();
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getReadinessProbe()).isNotNull();
    }

    @Test
    public void bridgeExecutorServiceTemplateIsProvided() {
        mockResourceForOwnerReference();

        TemplateProviderV1 templateProvider = new TemplateProviderV1Impl();
        Service service = templateProvider.loadBridgeExecutorServiceTemplate(hasMetadata, TemplateImportConfig.withDefaults(LabelsBuilder.V1_OPERATOR_NAME));

        assertOwnerReference(hasMetadata, service.getMetadata());
        assertLabels(service.getMetadata(), EXECUTOR_COMPONENT_NAME);
        assertThat(service.getSpec().getPorts().size()).isEqualTo(1);
        assertThat(service.getSpec().getPorts().get(0).getName()).isEqualTo("web");
        assertThat(service.getSpec().getPorts().get(0).getPort()).isEqualTo(8080);
        assertThat(service.getSpec().getPorts().get(0).getTargetPort().getIntVal()).isEqualTo(8080);
        assertThat(service.getSpec().getPorts().get(0).getProtocol()).isEqualTo("TCP");
    }

    private void assertLabels(ObjectMeta meta, String component) {
        assertThat(meta.getLabels().get(LabelsBuilder.COMPONENT_LABEL)).isEqualTo(component);
        assertThat(meta.getLabels().get(LabelsBuilder.MANAGED_BY_LABEL)).isEqualTo(LabelsBuilder.V1_OPERATOR_NAME);
        assertThat(meta.getLabels().get(LabelsBuilder.CREATED_BY_LABEL)).isEqualTo(LabelsBuilder.V1_OPERATOR_NAME);
    }

    private void assertOwnerReference(HasMetadata resource, ObjectMeta meta) {
        assertThat(meta.getOwnerReferences().size()).isEqualTo(1);

        OwnerReference ownerReference = meta.getOwnerReferences().get(0);
        assertThat(ownerReference.getName()).isEqualTo(resource.getMetadata().getName());
        assertThat(ownerReference.getApiVersion()).isEqualTo(resource.getApiVersion());
        assertThat(ownerReference.getKind()).isEqualTo(resource.getKind());
        assertThat(ownerReference.getUid()).isEqualTo(resource.getMetadata().getUid());
    }
}
