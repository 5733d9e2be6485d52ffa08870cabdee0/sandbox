package com.redhat.service.smartevents.shard.operator.v2.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.shard.operator.core.networking.NetworkingService;
import com.redhat.service.smartevents.shard.operator.core.utils.EventSourceFactory;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.RetryInfo;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

@ApplicationScoped
public class ManagedBridgeController implements Reconciler<ManagedBridge>,
                                                EventSourceInitializer<ManagedBridge>,
                                                ErrorStatusHandler<ManagedBridge> {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    NetworkingService networkingService;

    @Override
    public UpdateControl<ManagedBridge> reconcile(ManagedBridge resource, Context context) {
        return null;
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
}
