package com.redhat.service.smartevents.shard.operator.providers;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.shard.operator.TestSupport;
import com.redhat.service.smartevents.shard.operator.networking.NetworkingConstants;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.openshift.api.model.Route;

import static org.assertj.core.api.Assertions.assertThat;

public class TemplateProviderTest {

    private static final BridgeIngress BRIDGE_INGRESS = BridgeIngress.fromBuilder()
            .withBridgeName("id")
            .withNamespace("ns")
            .withBridgeId("12345")
            .withCustomerId("12456")
            .build();

    private static final BridgeExecutor BRIDGE_EXECUTOR = BridgeExecutor.fromBuilder()
            .withProcessorType(ProcessorType.SINK)
            .withProcessorName("id")
            .withNamespace("ns")
            .withImageName("image:latest")
            .withBridgeId(TestSupport.BRIDGE_ID)
            .withCustomerId(TestSupport.CUSTOMER_ID)
            .withProcessorId("id")
            .withDefinition(new ProcessorDefinition())
            .build();

    @Test
    public void metadataIsUpdated() {
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Deployment deployment = templateProvider.loadBridgeExecutorDeploymentTemplate(BRIDGE_EXECUTOR, TemplateImportConfig.withDefaults());
        assertOwnerReference(BRIDGE_EXECUTOR, deployment.getMetadata());
        assertThat(deployment.getMetadata().getName()).isEqualTo(BRIDGE_EXECUTOR.getMetadata().getName());
        assertThat(deployment.getMetadata().getNamespace()).isEqualTo(BRIDGE_EXECUTOR.getMetadata().getNamespace());

        deployment = templateProvider.loadBridgeExecutorDeploymentTemplate(BRIDGE_EXECUTOR, new TemplateImportConfig().withNameFromParent());
        assertThat(deployment.getMetadata().getOwnerReferences()).isNull();
        assertThat(deployment.getMetadata().getName()).isEqualTo(BRIDGE_EXECUTOR.getMetadata().getName());
        assertThat(deployment.getMetadata().getNamespace()).isNull();

        deployment = templateProvider.loadBridgeExecutorDeploymentTemplate(BRIDGE_EXECUTOR, new TemplateImportConfig().withNamespaceFromParent());
        assertThat(deployment.getMetadata().getOwnerReferences()).isNull();
        assertThat(deployment.getMetadata().getName()).isNull();
        assertThat(deployment.getMetadata().getNamespace()).isEqualTo(BRIDGE_EXECUTOR.getMetadata().getNamespace());

        deployment = templateProvider.loadBridgeExecutorDeploymentTemplate(BRIDGE_EXECUTOR, new TemplateImportConfig().withOwnerReferencesFromParent());
        assertOwnerReference(BRIDGE_EXECUTOR, deployment.getMetadata());
        assertThat(deployment.getMetadata().getName()).isNull();
        assertThat(deployment.getMetadata().getNamespace()).isNull();
    }

    @Test
    public void bridgeExecutorDeploymentTemplateIsProvided() {
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Deployment deployment = templateProvider.loadBridgeExecutorDeploymentTemplate(BRIDGE_EXECUTOR, TemplateImportConfig.withDefaults());

        assertOwnerReference(BRIDGE_EXECUTOR, deployment.getMetadata());
        assertLabels(deployment.getMetadata(), BridgeExecutor.COMPONENT_NAME);
        assertThat(deployment.getSpec().getReplicas()).isEqualTo(1);
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getLivenessProbe()).isNotNull();
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getReadinessProbe()).isNotNull();
    }

    @Test
    public void bridgeExecutorServiceTemplateIsProvided() {
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Service service = templateProvider.loadBridgeExecutorServiceTemplate(BRIDGE_EXECUTOR, TemplateImportConfig.withDefaults());

        assertOwnerReference(BRIDGE_EXECUTOR, service.getMetadata());
        assertLabels(service.getMetadata(), BridgeExecutor.COMPONENT_NAME);
        assertThat(service.getSpec().getPorts().size()).isEqualTo(1);
        assertThat(service.getSpec().getPorts().get(0).getName()).isEqualTo("web");
        assertThat(service.getSpec().getPorts().get(0).getPort()).isEqualTo(8080);
        assertThat(service.getSpec().getPorts().get(0).getTargetPort().getIntVal()).isEqualTo(8080);
        assertThat(service.getSpec().getPorts().get(0).getProtocol()).isEqualTo("TCP");
    }

    // TODO: add test for ingress secret/configmap/istio/knative resource

    @Test
    public void bridgeIngressOpenshiftRouteTemplateIsProvided() {
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Route route = templateProvider.loadBridgeIngressOpenshiftRouteTemplate(BRIDGE_INGRESS, new TemplateImportConfig()
                .withNameFromParent()
                .withPrimaryResourceFromParent());

        assertThat(route.getMetadata().getOwnerReferences()).isNull();
        assertLabels(route.getMetadata(), BridgeIngress.COMPONENT_NAME);
        assertThat(route.getSpec().getTo().getKind()).isEqualTo("Service");
        assertThat(route.getSpec().getPort().getTargetPort().getIntVal()).isEqualTo(8080);
    }

    @Test
    public void bridgeIngressKubernetesIngressTemplateIsProvided() {
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Ingress ingress = templateProvider.loadBridgeIngressKubernetesIngressTemplate(BRIDGE_INGRESS, new TemplateImportConfig()
                .withNameFromParent()
                .withPrimaryResourceFromParent());

        assertThat(ingress.getMetadata().getOwnerReferences()).isNull();
        assertLabels(ingress.getMetadata(), BridgeIngress.COMPONENT_NAME);

        assertThat(ingress.getSpec().getRules().size()).isEqualTo(1);
        assertThat(ingress.getSpec().getRules().get(0).getHttp().getPaths().size()).isEqualTo(1);
        assertThat(ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getPathType()).isEqualTo(NetworkingConstants.K8S_INGRESS_PATH_TYPE);
    }

    private void assertLabels(ObjectMeta meta, String component) {
        assertThat(meta.getLabels().get(LabelsBuilder.COMPONENT_LABEL)).isEqualTo(component);
        assertThat(meta.getLabels().get(LabelsBuilder.MANAGED_BY_LABEL)).isEqualTo(LabelsBuilder.OPERATOR_NAME);
        assertThat(meta.getLabels().get(LabelsBuilder.CREATED_BY_LABEL)).isEqualTo(LabelsBuilder.OPERATOR_NAME);
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
