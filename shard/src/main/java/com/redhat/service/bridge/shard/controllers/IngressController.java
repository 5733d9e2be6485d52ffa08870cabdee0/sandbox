package com.redhat.service.bridge.shard.controllers;

import java.util.Collections;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.k8s.Action;
import com.redhat.service.bridge.infra.k8s.K8SBridgeConstants;
import com.redhat.service.bridge.infra.k8s.KubernetesClient;
import com.redhat.service.bridge.infra.k8s.KubernetesResourceType;
import com.redhat.service.bridge.infra.k8s.ResourceEvent;
import com.redhat.service.bridge.infra.k8s.crds.BridgeCustomResource;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.shard.ManagerSyncService;

import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressRuleValueBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackendBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackendBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressSpec;
import io.fabric8.kubernetes.api.model.networking.v1.IngressSpecBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressStatus;
import io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPortBuilder;

@ApplicationScoped
public class IngressController {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngressController.class);

    @Inject
    ManagerSyncService managerSyncService;

    @Inject
    KubernetesClient kubernetesClient;

    void onEvent(@Observes ResourceEvent event) { // equivalent of ResourceEventSource for operator sdk
        if (event.getSubject().equals(K8SBridgeConstants.BRIDGE_TYPE)) {

            /*
             * If the CRD is deleted, remove also all the other related resource
             *
             * If another dependent resource is deleted, the `reconcileExecutor` will catch the mismatch between the expected state and the current state.
             * It will redeploy the resources so to reach the expected status at the end.
             */
            if (event.getAction().equals(Action.DELETED) && event.getResourceType().equals(KubernetesResourceType.CUSTOM_RESOURCE)) {
                delete(event.getResourceId());
                return;
            }

            BridgeCustomResource resource = kubernetesClient.getCustomResource(event.getResourceId(), BridgeCustomResource.class);
            if (event.getAction().equals(Action.ERROR)) {
                LOGGER.error("[shard] Failed to deploy Deployment with id '{}'", resource.getId());
                notifyFailedDeployment(resource.getId());
                return;
            }

            reconcileIngress(resource);
        }
    }

    private void delete(String resourceId) {
        // Delete Deployment
        kubernetesClient.deleteDeployment(resourceId);

        // TODO: Delete service

        // Delete ingress
        kubernetesClient.deleteNetworkIngress(resourceId);
    }

    private void reconcileIngress(BridgeCustomResource customResource) {
        LOGGER.info("[shard] Ingress Bridge reconcyle loop called");

        String id = customResource.getId();

        // Create Ingress Bridge Deployment if it does not exists
        Deployment deployment = kubernetesClient.getDeployment(id);
        if (deployment == null) {
            LOGGER.info("[shard] There is no deployment for Ingress Bridge '{}'. Creating.", id);
            kubernetesClient.createOrUpdateDeployment(createIngressBridgeDeployment(id));
            return;
        }

        Optional<DeploymentCondition> optStatus = deployment.getStatus().getConditions().stream().filter(x -> x.getStatus().equals("Ready")).findFirst();

        if (!optStatus.isPresent()) {
            LOGGER.info("[shard] Ingress Bridge deployment for Bridge '{}' is not ready yet", id);
            return;
        }

        // TODO: Create service for deployment

        // Create Ingress/Route for Bridge if it does not exists
        Ingress ingress = kubernetesClient.getNetworkIngress(id);
        if (ingress == null) {
            LOGGER.info("[shard] There is no Network ingress for ingress Bridge '{}'. Creating.", id);
            Ingress ingressResource = buildIngress(id);
            kubernetesClient.createNetworkIngress(ingressResource);
            return;
        }

        if (!isNetworkIngressReady(ingress)) {
            LOGGER.info("[shard] Network ingress for ingress Bridge '{}' not ready yet", id);
            return;
        }

        String endpoint = extractEndpoint(ingress);

        // Update the custom resource if needed

        if (!customResource.getStatus().equals(BridgeStatus.AVAILABLE)) {
            customResource.setStatus(BridgeStatus.AVAILABLE);
            customResource.setEndpoint(endpoint);

            kubernetesClient.createOrUpdateCustomResource(customResource.getId(), customResource, K8SBridgeConstants.BRIDGE_TYPE);

            BridgeDTO dto = customResource.toDTO();
            managerSyncService.notifyBridgeStatusChange(dto).subscribe().with(
                    success -> LOGGER.info("[shard] Updating Bridge with id '{}' done", dto.getId()),
                    failure -> LOGGER.warn("[shard] Updating Bridge with id '{}' FAILED", dto.getId()));
        }
    }

    private Deployment createIngressBridgeDeployment(String id) {
        return new DeploymentBuilder() // TODO: Add kind, replicas, image etc.. Or even read it from a yaml
                .withMetadata(new ObjectMetaBuilder()
                        .withName(id)
                        .withLabels(Collections.singletonMap(K8SBridgeConstants.METADATA_TYPE, K8SBridgeConstants.BRIDGE_TYPE))
                        .build())
                .build();
    }

    private Ingress buildIngress(String id) {

        IngressBackend ingressBackend = new IngressBackendBuilder()
                .withService(new IngressServiceBackendBuilder()
                        .withName("ingress-service") // just a placeholder
                        .withPort(new ServiceBackendPortBuilder().withNumber(80).build())
                        .build())
                .build();

        HTTPIngressPath httpIngressPath = new HTTPIngressPathBuilder()
                .withBackend(ingressBackend)
                .withPath("/ingress/events/" + id)
                .withPathType("Prefix")
                .build();

        IngressRule ingressRule = new IngressRuleBuilder()
                .withHttp(new HTTPIngressRuleValueBuilder()
                        .withPaths(httpIngressPath)
                        .build())
                .build();

        IngressSpec ingressSpec = new IngressSpecBuilder()
                .withRules(ingressRule)
                .build();

        return new IngressBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withName(id)
                                .withLabels(Collections.singletonMap(K8SBridgeConstants.METADATA_TYPE, K8SBridgeConstants.BRIDGE_TYPE))
                                .build())
                .withSpec(ingressSpec)
                .build();
    }

    public boolean isNetworkIngressReady(Ingress ingress) {
        return Optional.ofNullable(ingress)
                .map(Ingress::getStatus)
                .map(IngressStatus::getLoadBalancer)
                .map(LoadBalancerStatus::getIngress)
                .flatMap(x -> {
                    if (!x.isEmpty()) {
                        return Optional.of(x.get(0));
                    }
                    return Optional.empty();
                })
                .map(LoadBalancerIngress::getIp)
                .isPresent();
    }

    private String extractEndpoint(Ingress ingress) {
        return ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getPath();
    }

    private void notifyFailedDeployment(String id) {
        BridgeCustomResource customResource = kubernetesClient.getCustomResource(id, BridgeCustomResource.class);
        customResource.setStatus(BridgeStatus.FAILED);
        BridgeDTO dto = customResource.toDTO();
        managerSyncService.notifyBridgeStatusChange(dto).subscribe().with(
                success -> LOGGER.info("[shard] Updating Bridge with id '{}' done", dto.getId()),
                failure -> LOGGER.warn("[shard] Updating Bridge with id '{}' FAILED", dto.getId()));
    }

}
