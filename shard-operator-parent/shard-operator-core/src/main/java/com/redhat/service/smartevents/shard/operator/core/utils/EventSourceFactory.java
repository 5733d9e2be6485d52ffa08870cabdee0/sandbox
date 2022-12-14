package com.redhat.service.smartevents.shard.operator.core.utils;

import java.util.Set;

import com.redhat.service.smartevents.shard.operator.core.resources.istio.authorizationpolicy.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBroker;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;

import static java.util.Collections.EMPTY_SET;

public class EventSourceFactory {

    public static EventSource buildSecretsInformer(EventSourceContext<?> context, String operatorName, String componentName) {
        return new InformerEventSource<>(InformerConfiguration.from(Secret.class, context)
                .withLabelSelector(buildSelectorLabel(operatorName, componentName))
                .withSecondaryToPrimaryMapper(Mappers.fromOwnerReference())
                .build(), context);
    }

    public static EventSource buildConfigMapsInformer(EventSourceContext<?> context, String operatorName, String componentName) {
        return new InformerEventSource<>(InformerConfiguration.from(ConfigMap.class, context)
                .withLabelSelector(buildSelectorLabel(operatorName, componentName))
                .withSecondaryToPrimaryMapper(Mappers.fromOwnerReference())
                .build(), context);
    }

    // As the authorizationPolicy is not deployed in the same namespace of the CR we have to set the annotations with the primary resource references
    public static EventSource buildAuthorizationPolicyInformer(EventSourceContext<?> context, String operatorName, String componentName) {
        return new InformerEventSource<>(InformerConfiguration.from(AuthorizationPolicy.class, context)
                .withLabelSelector(buildSelectorLabel(operatorName, componentName))
                .withSecondaryToPrimaryMapper(EventSourceFactory::buildPrimaryResourcesRetriever)
                .build(), context);
    }

    public static EventSource buildBrokerInformer(EventSourceContext<?> context, String operatorName, String componentName) {
        return new InformerEventSource<>(InformerConfiguration.from(KnativeBroker.class, context)
                .withLabelSelector(buildSelectorLabel(operatorName, componentName))
                .withSecondaryToPrimaryMapper(Mappers.fromOwnerReference())
                .build(), context);
    }

    public static EventSource buildDeploymentsInformer(EventSourceContext<?> context, String operatorName, String componentName) {
        return new InformerEventSource<>(InformerConfiguration.from(Deployment.class, context)
                .withLabelSelector(buildSelectorLabel(operatorName, componentName))
                .withSecondaryToPrimaryMapper(Mappers.fromOwnerReference())
                .build(), context);
    }

    public static EventSource buildServicesInformer(EventSourceContext<?> context, String operatorName, String componentName) {
        return new InformerEventSource<>(InformerConfiguration.from(Service.class, context)
                .withLabelSelector(buildSelectorLabel(operatorName, componentName))
                .withSecondaryToPrimaryMapper(Mappers.fromOwnerReference())
                .build(), context);
    }

    public static EventSource buildServicesMonitorInformer(EventSourceContext<?> context, String operatorName, String componentName) {
        return new InformerEventSource<>(InformerConfiguration.from(ServiceMonitor.class, context)
                .withLabelSelector(buildSelectorLabel(operatorName, componentName))
                .withSecondaryToPrimaryMapper(Mappers.fromOwnerReference())
                .build(), context);
    }

    // As the Route is targeting the istio-gateway and is not deployed in the same namespace of the CR we have to set the annotations with the primary resource references
    public static EventSource buildRoutesInformer(EventSourceContext<?> context, String operatorName, String componentName) {
        return new InformerEventSource<>(InformerConfiguration.from(Route.class, context)
                .withLabelSelector(buildSelectorLabel(operatorName, componentName))
                .withSecondaryToPrimaryMapper(EventSourceFactory::buildPrimaryResourcesRetriever)
                .build(), context);
    }

    // As the Ingress is targeting the istio-gateway and is not deployed in the same namespace of the CR we have to set the annotations with the primary resource references
    public static EventSource buildIngressesInformer(EventSourceContext<?> context, String operatorName, String componentName) {
        return new InformerEventSource<>(InformerConfiguration.from(Ingress.class, context)
                .withLabelSelector(buildSelectorLabel(operatorName, componentName))
                .withSecondaryToPrimaryMapper(EventSourceFactory::buildPrimaryResourcesRetriever)
                .build(), context);
    }

    private static Set buildPrimaryResourcesRetriever(HasMetadata resource) {
        String name = resource.getMetadata().getAnnotations().get(LabelsBuilder.PRIMARY_RESOURCE_NAME_LABEL);
        String namespace = resource.getMetadata().getAnnotations().get(LabelsBuilder.PRIMARY_RESOURCE_NAMESPACE_LABEL);
        if (name != null && namespace != null) {
            return Set.of(new ResourceID(name, namespace));
        } else {
            return EMPTY_SET;
        }
    }

    private static String buildSelectorLabel(String operatorName, String componentName) {
        return LabelsBuilder.MANAGED_BY_LABEL + "=" + operatorName + " && " + LabelsBuilder.COMPONENT_LABEL + "=" + componentName;
    }
}
