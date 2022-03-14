package com.redhat.service.bridge.shard.operator.utils;

import java.util.Map;

import com.redhat.service.bridge.shard.operator.monitoring.ServiceMonitorClient;
import com.redhat.service.bridge.shard.operator.resources.AuthorizationPolicy;
import com.redhat.service.bridge.shard.operator.resources.KnativeBroker;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;

public class EventSourceFactory {

    public static EventSource buildAuthorizationPolicyInformer(KubernetesClient kubernetesClient, String componentName) {
        SharedIndexInformer<AuthorizationPolicy> authorizationPolicyInformer =
                kubernetesClient
                        .resources(AuthorizationPolicy.class)
                        .inAnyNamespace()
                        .withLabels(buildLabels(componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(authorizationPolicyInformer, Mappers.fromOwnerReference());
    }

    public static EventSource buildBrokerInformer(KubernetesClient kubernetesClient, String componentName) {
        SharedIndexInformer<KnativeBroker> knativeBrokerInformer =
                kubernetesClient
                        .resources(KnativeBroker.class)
                        .inAnyNamespace()
                        .withLabels(buildLabels(componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(knativeBrokerInformer, Mappers.fromOwnerReference());
    }

    public static EventSource buildConfigMapsInformer(KubernetesClient kubernetesClient, String componentName) {
        SharedIndexInformer<ConfigMap> configMapsInformer =
                kubernetesClient
                        .configMaps()
                        .inAnyNamespace()
                        .withLabels(buildLabels(componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(configMapsInformer, Mappers.fromOwnerReference());
    }

    public static EventSource buildSecretsInformer(KubernetesClient kubernetesClient, String componentName) {
        SharedIndexInformer<Secret> secretsInformer =
                kubernetesClient
                        .secrets()
                        .inAnyNamespace()
                        .withLabels(buildLabels(componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(secretsInformer, Mappers.fromOwnerReference());
    }

    public static EventSource buildDeploymentsInformer(KubernetesClient kubernetesClient, String componentName) {
        SharedIndexInformer<Deployment> deploymentsInformer =
                kubernetesClient
                        .apps()
                        .deployments()
                        .inAnyNamespace()
                        .withLabels(buildLabels(componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(deploymentsInformer, Mappers.fromOwnerReference());
    }

    public static EventSource buildServicesInformer(KubernetesClient kubernetesClient, String componentName) {
        SharedIndexInformer<Service> serviceInformer =
                kubernetesClient
                        .services()
                        .inAnyNamespace()
                        .withLabels(buildLabels(componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(serviceInformer, Mappers.fromOwnerReference());
    }

    public static EventSource buildServicesMonitorInformer(KubernetesClient kubernetesClient, String componentName) {
        SharedIndexInformer<ServiceMonitor> serviceMonitorInformer =
                ServiceMonitorClient.get(kubernetesClient)
                        .inAnyNamespace()
                        .withLabels(buildLabels(componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(serviceMonitorInformer, Mappers.fromOwnerReference());
    }

    public static EventSource buildRoutesInformer(OpenShiftClient client, String componentName) {
        SharedIndexInformer<Route> routesInformer =
                client.routes().inAnyNamespace()
                        .withLabels(buildLabels(componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(routesInformer, Mappers.fromOwnerReference());
    }

    public static EventSource buildIngressesInformer(KubernetesClient kubernetesClient, String componentName) {
        SharedIndexInformer<Ingress> ingressesInformer =
                kubernetesClient.network().v1().ingresses().inAnyNamespace()
                        .withLabels(buildLabels(componentName))
                        .runnableInformer(0);

        return new InformerEventSource<>(ingressesInformer, Mappers.fromOwnerReference());
    }

    private static Map<String, String> buildLabels(String componentName) {
        return new LabelsBuilder()
                .withManagedByOperator()
                .withComponent(componentName)
                .build();
    }
}
