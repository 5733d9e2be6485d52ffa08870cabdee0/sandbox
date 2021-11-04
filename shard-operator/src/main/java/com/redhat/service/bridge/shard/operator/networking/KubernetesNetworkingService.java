package com.redhat.service.bridge.shard.operator.networking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;
import com.redhat.service.bridge.shard.operator.watchers.networking.KubernetesIngressEventSource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Service;
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
import io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPortBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;

public class KubernetesNetworkingService implements NetworkingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkingService.class);
    private static final String PATH_REGEX = "(/|$)(.*)";
    private static final String NGINX_REWRITE_TARGET_ANNOTATION = "nginx.ingress.kubernetes.io/rewrite-target";
    private static final String REWRITE_TARGET_PLACEHOLDER = "/$2";

    private final KubernetesClient client;

    public KubernetesNetworkingService(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public AbstractEventSource createAndRegisterWatchNetworkResource(String applicationType) {
        return KubernetesIngressEventSource.createAndRegisterWatch(client, applicationType);
    }

    @Override
    public NetworkResource fetchOrCreateNetworkIngress(Service service) {
        Ingress ingress = client.network().v1().ingresses().inNamespace(service.getMetadata().getNamespace()).withName(service.getMetadata().getName()).get();

        if (ingress == null) {
            ingress = buildIngress(service);
            client.network().v1().ingresses().inNamespace(service.getMetadata().getNamespace()).withName(service.getMetadata().getName()).create(ingress);
        }

        return buildNetworkingResource(ingress);
    }

    @Override
    public boolean delete(String name, String namespace) {
        try {
            return client.network().v1().ingresses().inNamespace(namespace).withName(name).delete();
        } catch (Exception e) {
            LOGGER.debug("Can't delete ingress with name {} because it does not exist", name);
            return false;
        }
    }

    private Ingress buildIngress(Service service) {

        ObjectMeta metadata = new ObjectMetaBuilder()
                .withOwnerReferences(service.getMetadata().getOwnerReferences())
                .withLabels(
                        new LabelsBuilder()
                                .withApplicationType(service.getMetadata().getLabels().get(LabelsBuilder.APPLICATION_TYPE_LABEL))
                                .buildWithDefaults())
                .addToAnnotations(NGINX_REWRITE_TARGET_ANNOTATION, REWRITE_TARGET_PLACEHOLDER)
                .withName(service.getMetadata().getName())
                .build();

        IngressBackend ingressBackend = new IngressBackendBuilder()
                .withService(new IngressServiceBackendBuilder()
                        .withName(service.getMetadata().getName())
                        .withPort(new ServiceBackendPortBuilder().withNumber(service.getSpec().getPorts().get(0).getPort()).build())
                        .build())
                .build();

        HTTPIngressPath httpIngressPath = new HTTPIngressPathBuilder()
                .withBackend(ingressBackend)
                .withPath("/" + service.getMetadata().getName() + PATH_REGEX)
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

        return new IngressBuilder().withMetadata(metadata).withSpec(ingressSpec).build();
    }

    private NetworkResource buildNetworkingResource(Ingress ingress) {
        if (ingress.getStatus() == null || ingress.getStatus().getLoadBalancer() == null || ingress.getStatus().getLoadBalancer().getIngress() == null
                || ingress.getStatus().getLoadBalancer().getIngress().isEmpty() || ingress.getStatus().getLoadBalancer().getIngress().get(0).getIp() == null) {
            LOGGER.info("Ingress {} not ready yet", ingress.getMetadata().getName());
            return new NetworkResource("", false);
        }
        String host = ingress.getStatus().getLoadBalancer().getIngress().get(0).getIp();
        String endpoint = NetworkingConstants.HTTP_SCHEME + host + ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getPath().replace(PATH_REGEX, "");
        return new NetworkResource(endpoint, true);
    }
}
