package com.redhat.service.bridge.shard.operator.providers;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.shard.operator.networking.KubernetesNetworkingService;
import com.redhat.service.bridge.shard.operator.resources.BridgeExecutor;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.openshift.api.model.Route;

import static org.assertj.core.api.Assertions.assertThat;

public class TemplateProviderTest {

    private static final BridgeExecutor BRIDGE_EXECUTOR = buildBridgeExecutor();

    private static final BridgeIngress BRIDGE_INGRESS = BridgeIngress.fromBuilder()
            .withBridgeName("id")
            .withNamespace("ns")
            .withImageName("image:latest")
            .withBridgeId("12345")
            .withCustomerId("12456")
            .build();

    @Test
    public void bridgeIngressDeploymentTemplateIsProvided() {
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Deployment deployment = templateProvider.loadBridgeIngressDeploymentTemplate(BRIDGE_INGRESS);

        assertOwnerReference(BRIDGE_INGRESS, deployment.getMetadata());
        assertLabels(deployment.getMetadata(), BridgeIngress.COMPONENT_NAME);
        assertThat(deployment.getSpec().getReplicas()).isEqualTo(1);
    }

    @Test
    public void bridgeIngressServiceTemplateIsProvided() {
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Service service = templateProvider.loadBridgeIngressServiceTemplate(BRIDGE_INGRESS);

        assertOwnerReference(BRIDGE_INGRESS, service.getMetadata());
        assertLabels(service.getMetadata(), BridgeIngress.COMPONENT_NAME);
        assertThat(service.getSpec().getPorts().size()).isEqualTo(1);
        assertThat(service.getSpec().getPorts().get(0).getName()).isEqualTo("web");
        assertThat(service.getSpec().getPorts().get(0).getPort()).isEqualTo(8080);
        assertThat(service.getSpec().getPorts().get(0).getTargetPort().getIntVal()).isEqualTo(8080);
        assertThat(service.getSpec().getPorts().get(0).getProtocol()).isEqualTo("TCP");
    }

    @Test
    public void bridgeExecutorDeploymentTemplateIsProvided() {
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Deployment deployment = templateProvider.loadBridgeExecutorDeploymentTemplate(BRIDGE_EXECUTOR);

        assertOwnerReference(BRIDGE_EXECUTOR, deployment.getMetadata());
        assertLabels(deployment.getMetadata(), BridgeExecutor.COMPONENT_NAME);
        assertThat(deployment.getSpec().getReplicas()).isEqualTo(1);
    }

    @Test
    public void bridgeExecutorServiceTemplateIsProvided() {
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Service service = templateProvider.loadBridgeExecutorServiceTemplate(BRIDGE_EXECUTOR);

        assertOwnerReference(BRIDGE_EXECUTOR, service.getMetadata());
        assertLabels(service.getMetadata(), BridgeExecutor.COMPONENT_NAME);
        assertThat(service.getSpec().getPorts().size()).isEqualTo(1);
        assertThat(service.getSpec().getPorts().get(0).getName()).isEqualTo("web");
        assertThat(service.getSpec().getPorts().get(0).getPort()).isEqualTo(8080);
        assertThat(service.getSpec().getPorts().get(0).getTargetPort().getIntVal()).isEqualTo(8080);
        assertThat(service.getSpec().getPorts().get(0).getProtocol()).isEqualTo("TCP");
    }

    @Test
    public void bridgeIngressOpenshiftRouteTemplateIsProvided() {
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Route route = templateProvider.loadBridgeIngressOpenshiftRouteTemplate(BRIDGE_INGRESS);

        assertOwnerReference(BRIDGE_INGRESS, route.getMetadata());
        assertLabels(route.getMetadata(), BridgeIngress.COMPONENT_NAME);
        assertThat(route.getSpec().getTo().getKind()).isEqualTo("Service");
        assertThat(route.getSpec().getPort().getTargetPort().getIntVal()).isEqualTo(8080);
    }

    @Test
    public void bridgeIngressKubernetesIngressTemplateIsProvided() {
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Ingress ingress = templateProvider.loadBridgeIngressKubernetesIngressTemplate(BRIDGE_INGRESS);

        assertOwnerReference(BRIDGE_INGRESS, ingress.getMetadata());
        assertLabels(ingress.getMetadata(), BridgeIngress.COMPONENT_NAME);
        assertThat(ingress.getMetadata().getAnnotations().get(KubernetesNetworkingService.NGINX_REWRITE_TARGET_ANNOTATION)).isEqualTo(KubernetesNetworkingService.REWRITE_TARGET_PLACEHOLDER);

        assertThat(ingress.getSpec().getRules().size()).isEqualTo(1);
        assertThat(ingress.getSpec().getRules().get(0).getHttp().getPaths().size()).isEqualTo(1);
        assertThat(ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getPathType()).isEqualTo("Prefix");
    }

    private void assertLabels(ObjectMeta meta, String component) {
        assertThat(meta.getLabels().size()).isEqualTo(3);
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

    private static BridgeExecutor buildBridgeExecutor() {
        ObjectMeta meta = new ObjectMetaBuilder()
                .withName("id")
                .withNamespace("ns")
                .build();

        BridgeExecutor bridgeExecutor = new BridgeExecutor();
        bridgeExecutor.setMetadata(meta);

        return bridgeExecutor;
    }
}
