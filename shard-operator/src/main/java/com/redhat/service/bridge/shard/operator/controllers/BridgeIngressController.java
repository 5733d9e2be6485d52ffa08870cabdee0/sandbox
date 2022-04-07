package com.redhat.service.bridge.shard.operator.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.exceptions.BridgeErrorService;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.shard.operator.BridgeIngressService;
import com.redhat.service.bridge.shard.operator.ManagerSyncService;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.resources.ConditionReason;
import com.redhat.service.bridge.shard.operator.resources.ConditionType;
import com.redhat.service.bridge.shard.operator.resources.istio.AuthorizationPolicy;
import com.redhat.service.bridge.shard.operator.resources.knative.KnativeBroker;
import com.redhat.service.bridge.shard.operator.utils.EventSourceFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

@ApplicationScoped
@ControllerConfiguration
public class BridgeIngressController implements Reconciler<BridgeIngress>,
        EventSourceInitializer<BridgeIngress> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeIngressController.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ManagerSyncService managerSyncService;

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    BridgeErrorService bridgeErrorService;

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<BridgeIngress> eventSourceContext) {

        List<EventSource> eventSources = new ArrayList<>();
        eventSources.add(EventSourceFactory.buildSecretsInformer(kubernetesClient, BridgeIngress.COMPONENT_NAME));
        eventSources.add(EventSourceFactory.buildConfigMapsInformer(kubernetesClient, BridgeIngress.COMPONENT_NAME));
        eventSources.add(EventSourceFactory.buildBrokerInformer(kubernetesClient, BridgeIngress.COMPONENT_NAME));
        eventSources.add(EventSourceFactory.buildAuthorizationPolicyInformer(kubernetesClient, BridgeIngress.COMPONENT_NAME));

        return eventSources;
    }

    @Override
    public UpdateControl<BridgeIngress> reconcile(BridgeIngress bridgeIngress, Context context) {
        LOGGER.debug("Create or update BridgeIngress: '{}' in namespace '{}'", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());

        Secret secret = bridgeIngressService.fetchBridgeIngressSecret(bridgeIngress);

        if (secret == null) {
            LOGGER.debug("Secrets for the BridgeIngress '{}' have been not created yet.", bridgeIngress.getMetadata().getName());
            return UpdateControl.noUpdate();
        }

        AuthorizationPolicy authorizationPolicy = bridgeIngressService.fetchOrCreateBridgeIngressAuthorizationPolicy(bridgeIngress);

        ConfigMap configMap = bridgeIngressService.fetchOrCreateBridgeIngressConfigMap(bridgeIngress, secret);

        KnativeBroker knativeBroker = bridgeIngressService.fetchOrCreateBridgeIngressBroker(bridgeIngress, configMap);

        if (knativeBroker.getStatus() == null || knativeBroker.getStatus().getAddress() == null || "".equals(knativeBroker.getStatus().getAddress())) {
            LOGGER.info("Knative broker resource BridgeIngress: '{}' in namespace '{}' is NOT ready", bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace());
            bridgeIngress.getStatus().markConditionFalse(ConditionType.Ready);
            bridgeIngress.getStatus().markConditionTrue(ConditionType.Augmentation, ConditionReason.NetworkResourceNotReady);
            return UpdateControl.updateStatus(bridgeIngress);
        }

        if (!bridgeIngress.getStatus().isReady() || !knativeBroker.getStatus().getAddress().equals(bridgeIngress.getStatus().getEndpoint())) {
            bridgeIngress.getStatus().setEndpoint(knativeBroker.getStatus().getAddress().getUrl());
            bridgeIngress.getStatus().markConditionTrue(ConditionType.Ready);
            bridgeIngress.getStatus().markConditionFalse(ConditionType.Augmentation);
            notifyManager(bridgeIngress, ManagedResourceStatus.READY);
            return UpdateControl.updateStatus(bridgeIngress);
        }

        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl cleanup(BridgeIngress bridgeIngress, Context context) {
        LOGGER.info("Deleted BridgeIngress: '{}' in namespace '{}'", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());

        // Linked resources are automatically deleted

        notifyManager(bridgeIngress, ManagedResourceStatus.DELETED);

        return DeleteControl.defaultDelete();
    }

    private void notifyDeploymentFailure(BridgeIngress bridgeIngress, String failureReason) {
        LOGGER.warn("Ingress deployment BridgeIngress: '{}' in namespace '{}' has failed with reason: '{}'",
                bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace(), failureReason);
        notifyManager(bridgeIngress, ManagedResourceStatus.FAILED);
    }

    private void notifyManager(BridgeIngress bridgeIngress, ManagedResourceStatus status) {
        BridgeDTO dto = bridgeIngress.toDTO();
        dto.setStatus(status);

        managerSyncService.notifyBridgeStatusChange(dto)
                .subscribe().with(
                        success -> LOGGER.info("Updating Bridge with id '{}' done", dto.getId()),
                        failure -> LOGGER.error("Updating Bridge with id '{}' FAILED", dto.getId()));
    }
}
