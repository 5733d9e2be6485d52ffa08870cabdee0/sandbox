package com.redhat.service.smartevents.shard.operator.v1.controllers;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.api.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorHelper;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorInstance;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.ProvisioningTimeOutException;
import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.shard.operator.core.metrics.OperatorMetricsService;
import com.redhat.service.smartevents.shard.operator.core.networking.NetworkResource;
import com.redhat.service.smartevents.shard.operator.core.networking.NetworkingService;
import com.redhat.service.smartevents.shard.operator.core.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionTypeConstants;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.authorizationpolicy.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.core.utils.EventSourceFactory;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v1.BridgeIngressService;
import com.redhat.service.smartevents.shard.operator.v1.ManagerClient;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeIngressStatus;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.FAILED;

@ApplicationScoped
@ControllerConfiguration(labelSelector = LabelsBuilder.V1_RECONCILER_LABEL_SELECTOR)
public class BridgeIngressController implements Reconciler<BridgeIngress>,
        EventSourceInitializer<BridgeIngress>,
        ErrorStatusHandler<BridgeIngress>,
        Cleaner<BridgeIngress> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeIngressController.class);

    @ConfigProperty(name = "event-bridge.ingress.deployment.timeout-seconds")
    int ingressTimeoutSeconds;

    @ConfigProperty(name = "event-bridge.ingress.poll-interval.milliseconds")
    int ingressPollIntervalMilliseconds;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ManagerClient managerClient;

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    NetworkingService networkingService;

    @Inject
    IstioGatewayProvider istioGatewayProvider;

    @Inject
    BridgeErrorHelper bridgeErrorHelper;

    @Inject
    OperatorMetricsService metricsService;

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<BridgeIngress> eventSourceContext) {
        return EventSourceInitializer.nameEventSources(EventSourceFactory.buildSecretsInformer(eventSourceContext, LabelsBuilder.V1_OPERATOR_NAME, BridgeIngress.COMPONENT_NAME),
                EventSourceFactory.buildConfigMapsInformer(eventSourceContext, LabelsBuilder.V1_OPERATOR_NAME, BridgeIngress.COMPONENT_NAME),
                EventSourceFactory.buildBrokerInformer(eventSourceContext, LabelsBuilder.V1_OPERATOR_NAME, BridgeIngress.COMPONENT_NAME),
                EventSourceFactory.buildAuthorizationPolicyInformer(eventSourceContext, LabelsBuilder.V1_OPERATOR_NAME, BridgeIngress.COMPONENT_NAME),
                networkingService.buildInformerEventSource(eventSourceContext, LabelsBuilder.V1_OPERATOR_NAME, BridgeIngress.COMPONENT_NAME));
    }

    @Override
    public UpdateControl<BridgeIngress> reconcile(BridgeIngress bridgeIngress, Context context) {
        LOGGER.info("Create or update BridgeIngress: '{}' in namespace '{}'",
                bridgeIngress.getMetadata().getName(),
                bridgeIngress.getMetadata().getNamespace());

        BridgeIngressStatus status = bridgeIngress.getStatus();

        if (!status.isReady() && isTimedOut(status)) {
            notifyManagerOfFailure(bridgeIngress,
                    new ProvisioningTimeOutException(String.format(ProvisioningTimeOutException.TIMEOUT_FAILURE_MESSAGE,
                            bridgeIngress.getClass().getSimpleName(),
                            bridgeIngress.getSpec().getId())));
            status.markConditionFalse(ConditionTypeConstants.READY);
            return UpdateControl.updateStatus(bridgeIngress);
        }

        Secret secret = bridgeIngressService.fetchBridgeIngressSecret(bridgeIngress);

        if (secret == null) {
            LOGGER.info("Secrets for the BridgeIngress '{}' have been not created yet.",
                    bridgeIngress.getMetadata().getName());
            if (!status.isConditionTypeFalse(ConditionTypeConstants.READY)) {
                status.markConditionFalse(ConditionTypeConstants.READY);
            }
            if (!status.isConditionTypeFalse(BridgeIngressStatus.SECRET_AVAILABLE)) {
                status.markConditionFalse(BridgeIngressStatus.SECRET_AVAILABLE);
            }
            return UpdateControl.updateStatus(bridgeIngress).rescheduleAfter(ingressPollIntervalMilliseconds);
        } else if (!status.isConditionTypeTrue(BridgeIngressStatus.SECRET_AVAILABLE)) {
            status.markConditionTrue(BridgeIngressStatus.SECRET_AVAILABLE);
        }

        // Nothing to check for ConfigMap
        ConfigMap configMap = bridgeIngressService.fetchOrCreateBridgeIngressConfigMap(bridgeIngress, secret);
        if (!status.isConditionTypeTrue(BridgeIngressStatus.CONFIG_MAP_AVAILABLE)) {
            status.markConditionTrue(BridgeIngressStatus.CONFIG_MAP_AVAILABLE);
        }

        KnativeBroker knativeBroker = bridgeIngressService.fetchOrCreateBridgeIngressBroker(bridgeIngress, configMap);
        String path = extractBrokerPath(knativeBroker);

        if (path == null) {
            LOGGER.info("Knative broker resource BridgeIngress: '{}' in namespace '{}' is NOT ready",
                    bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace());
            if (!status.isConditionTypeFalse(ConditionTypeConstants.READY)) {
                status.markConditionFalse(ConditionTypeConstants.READY);
            }
            if (!status.isConditionTypeFalse(BridgeIngressStatus.KNATIVE_BROKER_AVAILABLE)) {
                status.markConditionFalse(BridgeIngressStatus.KNATIVE_BROKER_AVAILABLE);
            }
            return UpdateControl.updateStatus(bridgeIngress).rescheduleAfter(ingressPollIntervalMilliseconds);
        } else if (!status.isConditionTypeTrue(BridgeIngressStatus.KNATIVE_BROKER_AVAILABLE)) {
            status.markConditionTrue(BridgeIngressStatus.KNATIVE_BROKER_AVAILABLE);
        }

        // Nothing to check for Authorization Policy
        bridgeIngressService.fetchOrCreateBridgeIngressAuthorizationPolicy(bridgeIngress, path);
        if (!status.isConditionTypeTrue(BridgeIngressStatus.AUTHORISATION_POLICY_AVAILABLE)) {
            status.markConditionTrue(BridgeIngressStatus.AUTHORISATION_POLICY_AVAILABLE);
        }

        NetworkResource networkResource = networkingService.fetchOrCreateBrokerNetworkIngress(bridgeIngress, secret, bridgeIngress.getSpec().getHost(), path);

        if (!networkResource.isReady()) {
            LOGGER.info("Ingress networking resource BridgeIngress: '{}' in namespace '{}' is NOT ready",
                    bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace());
            if (!status.isConditionTypeFalse(ConditionTypeConstants.READY)) {
                status.markConditionFalse(ConditionTypeConstants.READY);
            }
            if (!status.isConditionTypeFalse(BridgeIngressStatus.NETWORK_RESOURCE_AVAILABLE)) {
                status.markConditionFalse(BridgeIngressStatus.NETWORK_RESOURCE_AVAILABLE);
            }
            return UpdateControl.updateStatus(bridgeIngress).rescheduleAfter(ingressPollIntervalMilliseconds);
        } else if (!status.isConditionTypeTrue(BridgeIngressStatus.NETWORK_RESOURCE_AVAILABLE)) {
            status.markConditionTrue(BridgeIngressStatus.NETWORK_RESOURCE_AVAILABLE);
        }

        LOGGER.info("Ingress networking resource BridgeIngress: '{}' in namespace '{}' is ready", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());

        // Only issue a Status Update once.
        // This is a work-around for non-deterministic Unit Tests.
        // See https://issues.redhat.com/browse/MGDOBR-1002
        if (!status.isReady()) {
            metricsService.onOperationComplete(bridgeIngress, MetricsOperation.CONTROLLER_RESOURCE_PROVISION);
            status.markConditionTrue(ConditionTypeConstants.READY);
            notifyManager(bridgeIngress, ManagedResourceStatus.READY);
            return UpdateControl.updateStatus(bridgeIngress);
        }

        return UpdateControl.noUpdate();
    }

    private boolean isTimedOut(BridgeIngressStatus status) {
        Optional<Date> lastTransitionDate = status.getConditions()
                .stream()
                .filter(c -> Objects.nonNull(c.getLastTransitionTime()))
                .reduce((c1, c2) -> c1.getLastTransitionTime().after(c2.getLastTransitionTime()) ? c1 : c2)
                .map(Condition::getLastTransitionTime);

        if (lastTransitionDate.isEmpty()) {
            return false;
        }

        ZonedDateTime zonedLastTransition = ZonedDateTime.ofInstant(lastTransitionDate.get().toInstant(), ZoneOffset.UTC);
        ZonedDateTime zonedNow = ZonedDateTime.now(ZoneOffset.UTC);
        return zonedNow.minus(Duration.of(ingressTimeoutSeconds, ChronoUnit.SECONDS)).isAfter(zonedLastTransition);
    }

    @Override
    public DeleteControl cleanup(BridgeIngress bridgeIngress, Context<BridgeIngress> context) {
        LOGGER.info("Deleted BridgeIngress: '{}' in namespace '{}'", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());

        // Linked resources are automatically deleted except for Authorization Policy and the ingress due to https://github.com/istio/istio/issues/37221

        // Since the authorizationPolicy has to be in the istio-system namespace due to https://github.com/istio/istio/issues/37221
        // we can not set the owner reference. We have to delete the resource manually.
        kubernetesClient.resources(AuthorizationPolicy.class)
                .inNamespace(istioGatewayProvider.getIstioGatewayService().getMetadata().getNamespace()) // https://github.com/istio/istio/issues/37221
                .withName(bridgeIngress.getMetadata().getName())
                .delete();

        // Since the ingress for the gateway has to be in the istio-system namespace
        // we can not set the owner reference. We have to delete the resource manually.
        networkingService.delete(bridgeIngress.getMetadata().getName(), istioGatewayProvider.getIstioGatewayService().getMetadata().getNamespace());

        metricsService.onOperationComplete(bridgeIngress, MetricsOperation.CONTROLLER_RESOURCE_DELETE);
        notifyManager(bridgeIngress, ManagedResourceStatus.DELETED);

        return DeleteControl.defaultDelete();
    }

    @Override
    public ErrorStatusUpdateControl<BridgeIngress> updateErrorStatus(BridgeIngress bridgeIngress, Context<BridgeIngress> context, Exception e) {
        if (context.getRetryInfo().isPresent() && context.getRetryInfo().get().isLastAttempt()) {
            BridgeIngressStatus status = bridgeIngress.getStatus();
            status.markConditionFalse(ConditionTypeConstants.READY);

            LOGGER.warn("BridgeIngress: '{}' in namespace '{}' has failed with reason: '{}'",
                    bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace(),
                    e.getMessage());
            BridgeErrorInstance bei = bridgeErrorHelper.getBridgeErrorInstance(e);
            bridgeIngress.getStatus().setStatusFromBridgeError(bei);
            notifyManagerOfFailure(bridgeIngress, bei);
            return ErrorStatusUpdateControl.updateStatus(bridgeIngress);
        }
        return ErrorStatusUpdateControl.noStatusUpdate();
    }

    private void notifyManager(BridgeIngress bridgeIngress, ManagedResourceStatus status) {
        ManagedResourceStatusUpdateDTO updateDTO = new ManagedResourceStatusUpdateDTO(bridgeIngress.getSpec().getId(), bridgeIngress.getSpec().getCustomerId(), status);

        managerClient.notifyBridgeStatusChange(updateDTO)
                .subscribe().with(
                        success -> LOGGER.info("Updating Bridge with id '{}' done", updateDTO.getId()),
                        failure -> LOGGER.error("Updating Bridge with id '{}' FAILED", updateDTO.getId(), failure));
    }

    private void notifyManagerOfFailure(BridgeIngress bridgeIngress, Exception e) {
        BridgeErrorInstance bei = bridgeErrorHelper.getBridgeErrorInstance(e);
        notifyManagerOfFailure(bridgeIngress, bei);
    }

    private void notifyManagerOfFailure(BridgeIngress bridgeIngress, BridgeErrorInstance bei) {
        LOGGER.error("BridgeIngress: '{}' in namespace '{}' has failed with reason: '{}'",
                bridgeIngress.getMetadata().getName(),
                bridgeIngress.getMetadata().getNamespace(),
                bei.getReason());

        metricsService.onOperationFailed(bridgeIngress, MetricsOperation.CONTROLLER_RESOURCE_PROVISION);

        String id = bridgeIngress.getSpec().getId();
        String customerId = bridgeIngress.getSpec().getCustomerId();
        ManagedResourceStatusUpdateDTO dto = new ManagedResourceStatusUpdateDTO(id,
                customerId,
                FAILED,
                bei);

        managerClient.notifyBridgeStatusChange(dto)
                .subscribe().with(
                        success -> LOGGER.info("Updating Processor with id '{}' done", dto.getId()),
                        failure -> LOGGER.error("Updating Processor with id '{}' FAILED", dto.getId(), failure));
    }

    private String extractBrokerPath(KnativeBroker broker) {
        if (broker == null || broker.getStatus() == null || broker.getStatus().getAddress().getUrl() == null) {
            return null;
        }
        try {
            return new URL(broker.getStatus().getAddress().getUrl()).getPath();
        } catch (MalformedURLException e) {
            LOGGER.info("Could not extract URL of the broker of BridgeIngress '{}'", broker.getMetadata().getName());
            return null;
        }
    }

}
