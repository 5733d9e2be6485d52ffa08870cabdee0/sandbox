package com.redhat.service.bridge.shard.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class BridgeIngressServiceImpl implements BridgeIngressService {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @ConfigProperty(name = "event-bridge.ingress.image")
    String ingressImage;

    @Override
    public void createBridgeIngress(BridgeDTO bridgeDTO) {
        String namespace = getOrCreateNamespace(bridgeDTO.getCustomerId());

        kubernetesClient
                .resources(BridgeIngress.class)
                .inNamespace(namespace)
                .create(BridgeIngress.fromDTO(bridgeDTO, namespace, ingressImage));
    }

    @Override
    public void deleteBridgeIngress(BridgeDTO bridgeDTO) {
        String namespace = customerNamespaceProvider.resolveNamespace(bridgeDTO.getCustomerId());
        kubernetesClient
                .resources(BridgeIngress.class)
                .inNamespace(namespace)
                .delete(BridgeIngress.fromDTO(bridgeDTO, namespace, ingressImage));
    }

    @Override
    public Deployment getOrCreateBridgeIngressDeployment(BridgeIngress bridgeIngress) {
        Deployment deployment = kubernetesClient.apps().deployments().inNamespace(bridgeIngress.getMetadata().getNamespace()).withName(bridgeIngress.getMetadata().getName()).get();

        if (deployment != null) {
            return deployment;
        }

        deployment = new DeploymentBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withName(bridgeIngress.getMetadata().getName())
                                .withNamespace(bridgeIngress.getMetadata().getNamespace())
                                .withLabels(
                                        new LabelsBuilder()
                                                .withApplicationType(LabelsBuilder.BRIDGE_INGRESS_APPLICATION_TYPE)
                                                .build())
                                .withOwnerReferences(
                                        new OwnerReferenceBuilder()
                                                .withApiVersion(bridgeIngress.getApiVersion())
                                                .withKind(bridgeIngress.getKind())
                                                .withName(bridgeIngress.getMetadata().getName())
                                                .withUid(bridgeIngress.getMetadata().getUid())
                                                .build())
                                .build())
                .withSpec(
                        new DeploymentSpecBuilder()
                                .withReplicas(1)
                                .withTemplate(
                                        new PodTemplateSpecBuilder()
                                                .withMetadata(
                                                        new ObjectMetaBuilder()
                                                                .addToLabels(LabelsBuilder.INSTANCE_LABEL, bridgeIngress.getMetadata().getName())
                                                                .build())
                                                .withSpec(
                                                        new PodSpecBuilder()
                                                                .withContainers(
                                                                        new ContainerBuilder()
                                                                                .withImage(bridgeIngress.getSpec().getImage())
                                                                                .withName(LabelsBuilder.BRIDGE_INGRESS_APPLICATION_TYPE)
                                                                                .build())
                                                                .build())
                                                .build())
                                .withSelector(
                                        new LabelSelectorBuilder()
                                                .addToMatchLabels(LabelsBuilder.INSTANCE_LABEL, bridgeIngress.getMetadata().getName())
                                                .build())
                                .build())
                .build();

        return kubernetesClient.apps().deployments().inNamespace(bridgeIngress.getMetadata().getNamespace()).create(deployment);
    }

    // TODO: https://issues.redhat.com/browse/MGDOBR-92 manage namespaces in a different service to be injected here
    private String getOrCreateNamespace(String customerId) {
        String namespace = customerNamespaceProvider.resolveNamespace(customerId);
        if (kubernetesClient.namespaces().withName(namespace).get() == null) {
            Namespace ns = new NamespaceBuilder()
                    .withNewMetadata()
                    .withName(namespace)
                    .endMetadata()
                    .build();
            kubernetesClient.namespaces().create(ns);
        }
        return namespace;
    }
}
