package com.redhat.service.smartevents.shard.operator.controllers;

import com.redhat.service.smartevents.infra.metrics.MetricsOperation;
import com.redhat.service.smartevents.shard.operator.ManagerClient;
import com.redhat.service.smartevents.shard.operator.metrics.OperatorMetricsService;
import com.redhat.service.smartevents.shard.operator.networking.NetworkingService;
import com.redhat.service.smartevents.shard.operator.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.reconcilers.*;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.istio.authorizationpolicy.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.services.KnativeKafkaBrokerService;
import com.redhat.service.smartevents.shard.operator.services.ReconciliationResultService;
import com.redhat.service.smartevents.shard.operator.utils.EventSourceFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@ControllerConfiguration()
public class BridgeIngressController implements Reconciler<BridgeIngress>,
        EventSourceInitializer<BridgeIngress> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeIngressController.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ManagerClient managerClient;

    @Inject
    NetworkingService networkingService;

    @Inject
    IstioGatewayProvider istioGatewayProvider;

    @Inject
    OperatorMetricsService metricsService;

    @Inject
    KnativeKafkaBrokerSecretReconciler knativeKafkaBrokerSecretReconciler;

    @Inject
    KnativeKafkaBrokerConfigMapReconciler knativeKafkaBrokerConfigMapReconciler;

    @Inject
    KnativeKafkaBrokerReconciler knativeKafkaBrokerReconciler;

    @Inject
    IstioAuthorizationPolicyReconciler istioAuthorizationPolicyReconciler;

    @Inject
    BridgeIngressReconciler bridgeIngressReconciler;

    @Inject
    BridgeRouteReconciler bridgeRouteReconciler;

    @Inject
    KnativeKafkaBrokerService knativeKafkaBrokerService;

    @Inject
    ReconciliationResultService reconciliationResultService;

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

        try {
            knativeKafkaBrokerSecretReconciler.reconcile(bridgeIngress);

            knativeKafkaBrokerConfigMapReconciler.reconcile(bridgeIngress);

            knativeKafkaBrokerReconciler.reconcile(bridgeIngress);

            String path = knativeKafkaBrokerService.extractBrokerPath(bridgeIngress);

            bridgeIngressReconciler.reconcile(bridgeIngress, path);

            bridgeRouteReconciler.reconcile(bridgeIngress);

            istioAuthorizationPolicyReconciler.reconcile(bridgeIngress, path);

        } catch (RuntimeException e) {
            return reconciliationResultService.getReconciliationResultFor(bridgeIngress, e);
        }
        finally {
            //managerClient.notifyBridgeStatusChange(bridgeIngress.getSpec().getId(), bridgeIngress.getStatus().getConditions());
        }
        return reconciliationResultService.getReconciliationResult(bridgeIngress);
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

        metricsService.onOperationComplete(bridgeIngress, MetricsOperation.CONTROLLER_RESOURCE_DELETE);
        //managerClient.notifyBridgeStatusChange(bridgeIngress.getSpec().getId(), bridgeIngress.getStatus().getConditions());

        return DeleteControl.defaultDelete();
    }
}
