package com.redhat.service.bridge.shard.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

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
                .create(BridgeIngress.fromDTO(bridgeDTO, ingressImage));
    }

    @Override
    public void deleteBridgeIngress(BridgeDTO bridgeDTO) {
        kubernetesClient
                .resources(BridgeIngress.class)
                .inNamespace(customerNamespaceProvider.resolveNamespace(bridgeDTO.getCustomerId()))
                .delete(BridgeIngress.fromDTO(bridgeDTO, ingressImage));
    }

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
