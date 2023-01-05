package com.redhat.service.smartevents.shard.operator.v2.controllers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.shard.operator.core.metrics.OperatorMetricsService;
import com.redhat.service.smartevents.shard.operator.core.networking.NetworkResource;
import com.redhat.service.smartevents.shard.operator.core.networking.NetworkingService;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.core.utils.EventSourceFactory;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v2.ManagedBridgeService;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridgeStatus;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.RetryInfo;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_AUTHORISATION_POLICY_READY_NAME;
import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_CONFIG_MAP_READY_NAME;
import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_KNATIVE_BROKER_READY_NAME;
import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_NETWORK_RESOURCE_READY_NAME;
import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_SECRET_READY_NAME;

@ApplicationScoped
@ControllerConfiguration(labelSelector = LabelsBuilder.V2_RECONCILER_LABEL_SELECTOR)
public class ManagedBridgeController implements Reconciler<ManagedBridge>,
        EventSourceInitializer<ManagedBridge>,
        ErrorStatusHandler<ManagedBridge> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedBridgeController.class);

    @ConfigProperty(name = "event-bridge.managed-bridge.poll.interval-milliseconds")
    int reconcileIntervalMillis;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    NetworkingService networkingService;

    @Inject
    ManagedBridgeService managedBridgeService;

    @Inject
    OperatorMetricsService metricsService;

    @Override
    public UpdateControl<ManagedBridge> reconcile(ManagedBridge managedBridge, Context context) {

        LOGGER.info("Reconciling ManagedBridge: '{}' in namespace '{}'",
                managedBridge.getMetadata().getName(),
                managedBridge.getMetadata().getNamespace());

        ManagedBridgeStatus status = managedBridge.getStatus();

        Secret secret = managedBridgeService.fetchBridgeSecret(managedBridge);
        if (secret == null) {
            status.markConditionFalse(DP_SECRET_READY_NAME);
            return handleFailure(managedBridge);
        } else {
            LOGGER.info("Secret for ManagedBridge with id '{}' has been created.", managedBridge.getSpec().getId());
            status.markConditionTrue(DP_SECRET_READY_NAME);
        }

        // Nothing to check for ConfigMap
        ConfigMap configMap = managedBridgeService.fetchOrCreateBridgeConfigMap(managedBridge, secret);
        status.markConditionTrue(DP_CONFIG_MAP_READY_NAME);

        KnativeBroker knativeBroker = managedBridgeService.fetchOrCreateKnativeBroker(managedBridge, configMap);
        String path = extractBrokerPath(knativeBroker);

        if (path == null) {
            LOGGER.info("The Knative Broker Resource for ManagedBridge '{}' in namespace '{}' is not ready",
                    managedBridge.getMetadata().getName(),
                    managedBridge.getMetadata().getNamespace());
            status.markConditionFalse(DP_KNATIVE_BROKER_READY_NAME);
            return handleFailure(managedBridge);
        } else {
            status.markConditionTrue(DP_KNATIVE_BROKER_READY_NAME);
            LOGGER.info("The Knative Broker Resource for ManagedBridge '{}' in namespace '{}' is ready", managedBridge.getMetadata().getName(), managedBridge.getMetadata().getNamespace());
        }

        // Nothing to check for Authorization Policy
        managedBridgeService.fetchOrCreateBridgeAuthorizationPolicy(managedBridge, path);
        status.markConditionTrue(DP_AUTHORISATION_POLICY_READY_NAME);

        NetworkResource networkResource = networkingService.fetchOrCreateBrokerNetworkIngress(managedBridge, secret, managedBridge.getSpec().getDnsConfiguration().getHost(), path);
        if (!networkResource.isReady()) {
            LOGGER.info("Ingress networking resource for ManagedBridge with id '{}' in namespace '{}' is not ready.",
                    managedBridge.getMetadata().getName(),
                    managedBridge.getMetadata().getNamespace());
            status.markConditionFalse(DP_NETWORK_RESOURCE_READY_NAME);
            return handleFailure(managedBridge);
        } else {
            LOGGER.info("Ingress networking resource for ManagedBridge with id '{}' in namespace '{}' is ready.", managedBridge.getMetadata().getName(),
                    managedBridge.getMetadata().getNamespace());
            status.markConditionTrue(DP_NETWORK_RESOURCE_READY_NAME);
        }

        if (isBridgeStatusChange(managedBridge)) {
            metricsService.onOperationComplete(managedBridge, MetricsOperation.CONTROLLER_RESOURCE_PROVISION);
            return UpdateControl.updateStatus(managedBridge);
        }
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl cleanup(ManagedBridge resource, Context context) {
        return null;
    }

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<ManagedBridge> context) {

        List<EventSource> eventSources = new ArrayList<>();
        eventSources.add(EventSourceFactory.buildSecretsInformer(kubernetesClient, LabelsBuilder.V2_OPERATOR_NAME, ManagedBridge.COMPONENT_NAME));
        eventSources.add(EventSourceFactory.buildConfigMapsInformer(kubernetesClient, LabelsBuilder.V2_OPERATOR_NAME, ManagedBridge.COMPONENT_NAME));
        eventSources.add(EventSourceFactory.buildBrokerInformer(kubernetesClient, LabelsBuilder.V2_OPERATOR_NAME, ManagedBridge.COMPONENT_NAME));
        eventSources.add(EventSourceFactory.buildAuthorizationPolicyInformer(kubernetesClient, LabelsBuilder.V2_OPERATOR_NAME, ManagedBridge.COMPONENT_NAME));
        eventSources.add(networkingService.buildInformerEventSource(LabelsBuilder.V2_OPERATOR_NAME, ManagedBridge.COMPONENT_NAME));

        return eventSources;
    }

    @Override
    public Optional<ManagedBridge> updateErrorStatus(ManagedBridge resource, RetryInfo retryInfo, RuntimeException e) {
        return Optional.empty();
    }

    private String extractBrokerPath(KnativeBroker broker) {
        if (broker == null || broker.getStatus() == null || broker.getStatus().getAddress().getUrl() == null) {
            return null;
        }
        try {
            return new URL(broker.getStatus().getAddress().getUrl()).getPath();
        } catch (MalformedURLException e) {
            LOGGER.info("Could not extract URL of the broker of ManagedBridge '{}'", broker.getMetadata().getName());
            return null;
        }
    }

    private UpdateControl<ManagedBridge> handleFailure(ManagedBridge managedBridge) {
        return UpdateControl.updateStatus(managedBridge).rescheduleAfter(reconcileIntervalMillis);
    }

    private boolean isBridgeStatusChange(ManagedBridge updatedBridge) {
        ManagedBridge oldBridge = managedBridgeService.fetchManagedBridge(updatedBridge.getMetadata().getName(), updatedBridge.getMetadata().getNamespace());
        return !managedBridgeService.compareBridgeStatus(oldBridge, updatedBridge);
    }
}
