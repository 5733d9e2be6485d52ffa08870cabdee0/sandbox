package com.redhat.service.smartevents.shard.operator.core.utils;

import java.util.Map;
import java.util.Set;

import com.redhat.service.smartevents.shard.operator.core.monitoring.ServiceMonitorClient;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.authorizationpolicy.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBroker;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;

import static java.util.Collections.EMPTY_SET;

public class EventSourceFactory {

    // Can we use the generic method instead for all the other methods? And having the first three parameters as fields since they're identical
    public static <T extends HasMetadata> EventSource buildResourceInformer(KubernetesClient kubernetesClient, String operatorName, String componentName, Class<T> resourceType) {
        SharedIndexInformer<T> resourceInformer =
                kubernetesClient
                        .resources(resourceType)
                        .inAnyNamespace()
                        .withLabels(buildLabels(componentName, operatorName))
                        .runnableInformer(0);

        return new InformerEventSource<>(resourceInformer, Mappers.fromOwnerReference());
    }

    public static EventSource buildSecretsInformer(KubernetesClient kubernetesClient, String operatorName, String componentName) {
        SharedIndexInformer<Secret> secretsInformer =
                kubernetesClient
                        .secrets()
                        .inAnyNamespace()
                        .withLabels(buildLabels(operatorName, componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(secretsInformer, Mappers.fromOwnerReference());
    }

    public static EventSource buildConfigMapsInformer(KubernetesClient kubernetesClient, String operatorName, String componentName) {
        SharedIndexInformer<ConfigMap> configMapsInformer =
                kubernetesClient
                        .configMaps()
                        .inAnyNamespace()
                        .withLabels(buildLabels(operatorName, componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(configMapsInformer, Mappers.fromOwnerReference());
    }

    // As the authorizationPolicy is not deployed in the same namespace of the CR we have to set the annotations with the primary resource references
    public static EventSource buildAuthorizationPolicyInformer(KubernetesClient kubernetesClient, String operatorName, String componentName) {
        SharedIndexInformer<AuthorizationPolicy> authorizationPolicyInformer =
                kubernetesClient
                        .resources(AuthorizationPolicy.class)
                        .inAnyNamespace()
                        .withLabels(buildLabels(operatorName, componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(authorizationPolicyInformer, EventSourceFactory::buildPrimaryResourcesRetriever);
    }

    public static EventSource buildBrokerInformer(KubernetesClient kubernetesClient, String operatorName, String componentName) {
        SharedIndexInformer<KnativeBroker> knativeBrokerInformer =
                kubernetesClient
                        .resources(KnativeBroker.class)
                        .inAnyNamespace()
                        .withLabels(buildLabels(operatorName, componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(knativeBrokerInformer, Mappers.fromOwnerReference());
    }

    public static EventSource buildDeploymentsInformer(KubernetesClient kubernetesClient, String operatorName, String componentName) {
        SharedIndexInformer<Deployment> deploymentsInformer =
                kubernetesClient
                        .apps()
                        .deployments()
                        .inAnyNamespace()
                        .withLabels(buildLabels(operatorName, componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(deploymentsInformer, Mappers.fromOwnerReference());
    }

    public static EventSource buildServicesInformer(KubernetesClient kubernetesClient, String operatorName, String componentName) {
        SharedIndexInformer<Service> serviceInformer =
                kubernetesClient
                        .services()
                        .inAnyNamespace()
                        .withLabels(buildLabels(operatorName, componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(serviceInformer, Mappers.fromOwnerReference());
    }

    public static EventSource buildServicesMonitorInformer(KubernetesClient kubernetesClient, String operatorName, String componentName) {
        SharedIndexInformer<ServiceMonitor> serviceMonitorInformer =
                ServiceMonitorClient.get(kubernetesClient)
                        .inAnyNamespace()
                        .withLabels(buildLabels(operatorName, componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(serviceMonitorInformer, Mappers.fromOwnerReference());
    }

    // As the Route is targeting the istio-gateway and is not deployed in the same namespace of the CR we have to set the annotations with the primary resource references
    public static EventSource buildRoutesInformer(OpenShiftClient client, String operatorName, String componentName) {
        SharedIndexInformer<Route> routesInformer =
                client.routes().inAnyNamespace()
                        .withLabels(buildLabels(operatorName, componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(routesInformer, EventSourceFactory::buildPrimaryResourcesRetriever);
    }

    // As the Ingress is targeting the istio-gateway and is not deployed in the same namespace of the CR we have to set the annotations with the primary resource references
    public static EventSource buildIngressesInformer(KubernetesClient kubernetesClient, String operatorName, String componentName) {
        SharedIndexInformer<Ingress> ingressesInformer =
                kubernetesClient.network().v1().ingresses().inAnyNamespace()
                        .withLabels(buildLabels(operatorName, componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(ingressesInformer, EventSourceFactory::buildPrimaryResourcesRetriever);
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

    private static Map<String, String> buildLabels(String operatorName, String componentName) {
        return new LabelsBuilder()
                .withManagedByOperator(operatorName)
                .withComponent(componentName)
                .build();
    }
}
