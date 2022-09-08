package com.redhat.service.smartevents.shard.operator.controllers;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.exceptions.BridgeErrorHelper;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorInstance;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.ProvisioningTimeOutException;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.shard.operator.BridgeIngressService;
import com.redhat.service.smartevents.shard.operator.ManagerClient;
import com.redhat.service.smartevents.shard.operator.networking.NetworkResource;
import com.redhat.service.smartevents.shard.operator.networking.NetworkingService;
import com.redhat.service.smartevents.shard.operator.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngressStatus;
import com.redhat.service.smartevents.shard.operator.resources.Condition;
import com.redhat.service.smartevents.shard.operator.resources.ConditionReasonConstants;
import com.redhat.service.smartevents.shard.operator.resources.ConditionStatus;
import com.redhat.service.smartevents.shard.operator.resources.ConditionTypeConstants;
import com.redhat.service.smartevents.shard.operator.resources.istio.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.utils.EventSourceFactory;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;

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

import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.FAILED;

@ApplicationScoped
@ControllerConfiguration(labelSelector = LabelsBuilder.RECONCILER_LABEL_SELECTOR)
public class BridgeIngressController implements Reconciler<BridgeIngress>,
        EventSourceInitializer<BridgeIngress>, ErrorStatusHandler<BridgeIngress> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeIngressController.class);

    @ConfigProperty(name = "event-bridge.ingress.deployment.timeout-seconds")
    int ingressTimeoutSeconds;

    @ConfigProperty(name = "event-bridge.ingress.poll-interval.seconds")
    int ingressPollIntervalSeconds;

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

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<BridgeIngress> eventSourceContext) {

        List<EventSource> eventSources = new ArrayList<>();
        eventSources.add(EventSourceFactory.buildSecretsInformer(kubernetesClient, BridgeIngress.COMPONENT_NAME));
        eventSources.add(EventSourceFactory.buildConfigMapsInformer(kubernetesClient, BridgeIngress.COMPONENT_NAME));
        eventSources.add(EventSourceFactory.buildBrokerInformer(kubernetesClient, BridgeIngress.COMPONENT_NAME));
        eventSources.add(EventSourceFactory.buildAuthorizationPolicyInformer(kubernetesClient, BridgeIngress.COMPONENT_NAME));
        eventSources.add(networkingService.buildInformerEventSource(BridgeIngress.COMPONENT_NAME));

        return eventSources;
    }

    @Override
    public UpdateControl<BridgeIngress> reconcile(BridgeIngress bridgeIngress, Context context) {
        LOGGER.info("Create or update BridgeIngress: '{}' in namespace '{}'",
                bridgeIngress.getMetadata().getName(),
                bridgeIngress.getMetadata().getNamespace());

        // Set a "start" time on the creation
        BridgeIngressStatus status = bridgeIngress.getStatus();
        if (status.getConditionByType(ConditionTypeConstants.PROGRESSING).isPresent()) {
            if (status.getConditionByType(ConditionTypeConstants.PROGRESSING).get().getStatus() == ConditionStatus.Unknown) {
                status.markConditionTrue(ConditionTypeConstants.PROGRESSING, ConditionReasonConstants.DEPLOYMENT_INITIATED);
                return UpdateControl.updateStatus(bridgeIngress).rescheduleAfter(ingressPollIntervalSeconds);
            }
        }

        if (isTimedOut(status)) {
            notifyManagerOfFailure(bridgeIngress,
                    new ProvisioningTimeOutException(String.format(ProvisioningTimeOutException.TIMEOUT_FAILURE_MESSAGE,
                            bridgeIngress.getClass().getSimpleName(),
                            bridgeIngress.getSpec().getId())));
            status.markConditionFalse(ConditionTypeConstants.READY);
            status.markConditionFalse(ConditionTypeConstants.AUGMENTATION);
            status.markConditionFalse(ConditionTypeConstants.PROGRESSING);
            return UpdateControl.updateStatus(bridgeIngress);
        }

        Secret secret = bridgeIngressService.fetchBridgeIngressSecret(bridgeIngress);

        if (secret == null) {
            LOGGER.info("Secrets for the BridgeIngress '{}' have been not created yet.", bridgeIngress.getMetadata().getName());
            status.markConditionFalse(ConditionTypeConstants.READY);
            status.markConditionTrue(ConditionTypeConstants.AUGMENTATION, ConditionReasonConstants.SECRETS_NOT_FOUND);
            return UpdateControl.updateStatus(bridgeIngress).rescheduleAfter(ingressPollIntervalSeconds);
        } else {
            // Update "Last Transition Time", effectively resetting time-out.
            status.getConditionByType(ConditionTypeConstants.AUGMENTATION).ifPresent(c -> {
                if (Objects.equals(c.getReason(), ConditionReasonConstants.SECRETS_NOT_FOUND)) {
                    status.markConditionTrue(ConditionTypeConstants.PROGRESSING, ConditionReasonConstants.SECRETS_FOUND);
                }
            });
        }

        ConfigMap configMap = bridgeIngressService.fetchOrCreateBridgeIngressConfigMap(bridgeIngress, secret);

        // Nothing to check for ConfigMap

        KnativeBroker knativeBroker = bridgeIngressService.fetchOrCreateBridgeIngressBroker(bridgeIngress, configMap);
        String path = extractBrokerPath(knativeBroker);

        if (path == null) {
            LOGGER.info("Knative broker resource BridgeIngress: '{}' in namespace '{}' is NOT ready", bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace());
            status.markConditionFalse(ConditionTypeConstants.READY);
            status.markConditionTrue(ConditionTypeConstants.AUGMENTATION, ConditionReasonConstants.KNATIVE_BROKER_NOT_READY);
            return UpdateControl.updateStatus(bridgeIngress).rescheduleAfter(ingressPollIntervalSeconds);
        } else {
            // Update "Last Transition Time", effectively resetting time-out.
            status.getConditionByType(ConditionTypeConstants.AUGMENTATION).ifPresent(c -> {
                if (Objects.equals(c.getReason(), ConditionReasonConstants.KNATIVE_BROKER_NOT_READY)) {
                    status.markConditionTrue(ConditionTypeConstants.PROGRESSING, ConditionReasonConstants.KNATIVE_BROKER_READY);
                }
            });
        }

        bridgeIngressService.fetchOrCreateBridgeIngressAuthorizationPolicy(bridgeIngress, path);

        // Nothing to check for Authorization Policy

        NetworkResource networkResource = networkingService.fetchOrCreateBrokerNetworkIngress(bridgeIngress, secret, path);

        if (!networkResource.isReady()) {
            LOGGER.info("Ingress networking resource BridgeIngress: '{}' in namespace '{}' is NOT ready", bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace());
            status.markConditionFalse(ConditionTypeConstants.READY);
            status.markConditionTrue(ConditionTypeConstants.AUGMENTATION, ConditionReasonConstants.NETWORK_RESOURCE_NOT_READY);
            return UpdateControl.updateStatus(bridgeIngress).rescheduleAfter(ingressPollIntervalSeconds);
        } else {
            // Update "Last Transition Time", effectively resetting time-out.
            status.getConditionByType(ConditionTypeConstants.AUGMENTATION).ifPresent(c -> {
                if (Objects.equals(c.getReason(), ConditionReasonConstants.NETWORK_RESOURCE_NOT_READY)) {
                    status.markConditionTrue(ConditionTypeConstants.PROGRESSING, ConditionReasonConstants.NETWORK_RESOURCE_READY);
                }
            });
        }

        LOGGER.info("Ingress networking resource BridgeIngress: '{}' in namespace '{}' is ready", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());

        // Only issue a Status Update once.
        // This is a work-around for non-deterministic Unit Tests.
        // See https://issues.redhat.com/browse/MGDOBR-1002
        if (!status.isReady()) {
            status.markConditionTrue(ConditionTypeConstants.READY);
            status.markConditionFalse(ConditionTypeConstants.AUGMENTATION);
            status.markConditionFalse(ConditionTypeConstants.PROGRESSING);
            notifyManager(bridgeIngress, ManagedResourceStatus.READY);
            return UpdateControl.updateStatus(bridgeIngress).rescheduleAfter(ingressPollIntervalSeconds);
        }

        return UpdateControl.noUpdate();
    }

    private boolean isTimedOut(BridgeIngressStatus status) {
        Optional<Condition> progress = status.getConditionByType(ConditionTypeConstants.PROGRESSING);
        if (progress.isPresent()) {
            Date lastTransitionTime = progress.get().getLastTransitionTime();
            ZonedDateTime zonedLastTransition = ZonedDateTime.ofInstant(lastTransitionTime.toInstant(), ZoneOffset.UTC);
            ZonedDateTime zonedNow = ZonedDateTime.now(ZoneOffset.UTC);
            return zonedNow.minus(Duration.of(ingressTimeoutSeconds, ChronoUnit.SECONDS)).isAfter(zonedLastTransition);
        }
        return false;
    }

    @Override
    public DeleteControl cleanup(BridgeIngress bridgeIngress, Context context) {
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

        notifyManager(bridgeIngress, ManagedResourceStatus.DELETED);

        return DeleteControl.defaultDelete();
    }

    @Override
    public Optional<BridgeIngress> updateErrorStatus(BridgeIngress bridgeIngress, RetryInfo retryInfo, RuntimeException e) {
        if (retryInfo.isLastAttempt()) {
            LOGGER.warn("BridgeIngress: '{}' in namespace '{}' has failed with reason: '{}'",
                    bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace(),
                    e.getMessage());
            BridgeErrorInstance bei = bridgeErrorHelper.getBridgeErrorInstance(e);
            bridgeIngress.getStatus().setStatusFromBridgeError(bei);
            notifyManagerOfFailure(bridgeIngress, bei);
        }
        return Optional.of(bridgeIngress);
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
