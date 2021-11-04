package com.redhat.service.bridge.shard.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.bridge.shard.operator.providers.TemplateProvider;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class BridgeIngressServiceImpl implements BridgeIngressService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeIngressServiceImpl.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @Inject
    TemplateProvider templateProvider;

    @ConfigProperty(name = "event-bridge.ingress.image")
    String ingressImage;

    @Override
    public void createBridgeIngress(BridgeDTO bridgeDTO) {
        final Namespace namespace = customerNamespaceProvider.fetchOrCreateCustomerNamespace(bridgeDTO.getCustomerId());

        kubernetesClient
                .resources(BridgeIngress.class)
                .inNamespace(namespace.getMetadata().getName())
                .create(BridgeIngress.fromDTO(bridgeDTO, namespace.getMetadata().getName(), ingressImage));
    }

    @Override
    public Deployment fetchOrCreateBridgeIngressDeployment(BridgeIngress bridgeIngress) {
        Deployment deployment = kubernetesClient.apps().deployments().inNamespace(bridgeIngress.getMetadata().getNamespace()).withName(bridgeIngress.getMetadata().getName()).get();

        if (deployment != null) {
            return deployment;
        }

        deployment = templateProvider.loadIngressDeploymentTemplate();

        // Name and namespace
        deployment.getMetadata().setName(bridgeIngress.getMetadata().getName());
        deployment.getMetadata().setNamespace(bridgeIngress.getMetadata().getNamespace());

        // Labels
        deployment.getMetadata().setLabels(
                new LabelsBuilder()
                        .withComponent(LabelsBuilder.BRIDGE_INGRESS_COMPONENT)
                        .buildWithDefaults());

        // Owner reference
        deployment.getMetadata().getOwnerReferences().get(0).setKind(bridgeIngress.getKind());
        deployment.getMetadata().getOwnerReferences().get(0).setName(bridgeIngress.getMetadata().getName());
        deployment.getMetadata().getOwnerReferences().get(0).setApiVersion(bridgeIngress.getApiVersion());
        deployment.getMetadata().getOwnerReferences().get(0).setUid(bridgeIngress.getMetadata().getUid());

        // Specs
        deployment.getSpec().getSelector().setMatchLabels(new LabelsBuilder().withAppInstance(bridgeIngress.getMetadata().getName()).build());
        deployment.getSpec().getTemplate().getMetadata().setLabels(new LabelsBuilder().withAppInstance(bridgeIngress.getMetadata().getName()).build());
        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setName(LabelsBuilder.BRIDGE_INGRESS_COMPONENT);
        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(bridgeIngress.getSpec().getImage());

        return kubernetesClient.apps().deployments().inNamespace(bridgeIngress.getMetadata().getNamespace()).create(deployment);
    }

    @Override
    public void deleteBridgeIngress(BridgeDTO bridgeDTO) {
        final String namespace = customerNamespaceProvider.resolveName(bridgeDTO.getCustomerId());
        final boolean bridgeDeleted =
                kubernetesClient
                        .resources(BridgeIngress.class)
                        .inNamespace(namespace)
                        .delete(BridgeIngress.fromDTO(bridgeDTO, namespace, ingressImage));
        if (bridgeDeleted) {
            customerNamespaceProvider.deleteCustomerNamespaceIfEmpty(bridgeDTO.getCustomerId());
        } else {
            // TODO: we might need to review this use case and have a manager to look at a queue of objects not deleted and investigate. Unfortunately the API does not give us a reason.
            LOGGER.warn("BridgeIngress '{}' not deleted", bridgeDTO);
        }
    }
}
