package com.redhat.service.bridge.shard.operator.networking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.shard.operator.providers.TemplateProvider;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.utils.EventSourceFactory;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressRuleValueBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackendBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackendBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressSpec;
import io.fabric8.kubernetes.api.model.networking.v1.IngressSpecBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPortBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

public class KubernetesNetworkingService implements NetworkingService {

    public static final String NGINX_REWRITE_TARGET_ANNOTATION = "nginx.ingress.kubernetes.io/rewrite-target";
    public static final String REWRITE_TARGET_PLACEHOLDER = "/$2";

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkingService.class);
    private static final String PATH_REGEX = "(/|$)(.*)";
    private final KubernetesClient client;
    private final TemplateProvider templateProvider;

    public KubernetesNetworkingService(KubernetesClient client, TemplateProvider templateProvider) {
        this.client = client;
        this.templateProvider = templateProvider;
    }

    @Override
    public EventSource buildInformerEventSource(String component) {
        return EventSourceFactory.buildIngressesInformer(client, component);
    }

    @Override
    public NetworkResource fetchOrCreateNetworkIngress(BridgeIngress bridgeIngress, Service service) {
        Ingress expected = buildIngress(bridgeIngress, service);

        Ingress existing = client
                .network()
                .v1()
                .ingresses()
                .inNamespace(service.getMetadata().getNamespace())
                .withName(bridgeIngress.getMetadata().getName())
                .get();
        if (existing != null) {
            LOGGER.info("exists");
            LOGGER.info(existing.toString());
        }
        if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            LOGGER.info("Don't exist");
            client
                    .network()
                    .v1()
                    .ingresses()
                    .inNamespace(service.getMetadata().getNamespace())
                    .withName(bridgeIngress.getMetadata().getName())
                    .createOrReplace(expected);
            return buildNetworkingResource(expected);
        }

        return buildNetworkingResource(existing);
    }

    @Override
    public boolean delete(String name, String namespace) {
        try {
            return client.network().v1().ingresses().inNamespace(namespace).withName(name).delete();
        } catch (Exception e) {
            LOGGER.debug("Can't delete ingress with name '{}' because it does not exist", name);
            return false;
        }
    }

    private Ingress buildIngress(BridgeIngress bridgeIngress, Service service) {
        Ingress ingress = templateProvider.loadBridgeIngressKubernetesIngressTemplate(bridgeIngress);

        // TODO: refactor
        ingress.getMetadata().getAnnotations().replace("nginx.ingress.kubernetes.io/rewrite-target", "/default/ob-" + bridgeIngress.getSpec().getId().substring(0, 5));

        IngressBackend ingressBackend = new IngressBackendBuilder()
                .withService(new IngressServiceBackendBuilder()
                        .withName(service.getMetadata().getName())
                        .withPort(new ServiceBackendPortBuilder().withNumber(service.getSpec().getPorts().get(0).getPort()).build())
                        .build())
                .build();

        HTTPIngressPath httpIngressPath = new HTTPIngressPathBuilder()
                .withBackend(ingressBackend)
                // TODO: refactor
                .withPath("/" + bridgeIngress.getMetadata().getName())
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

        ingress.setSpec(ingressSpec);

        return ingress;
    }

    private NetworkResource buildNetworkingResource(Ingress ingress) {
        if (ingress.getStatus() == null || ingress.getStatus().getLoadBalancer() == null || ingress.getStatus().getLoadBalancer().getIngress() == null
                || ingress.getStatus().getLoadBalancer().getIngress().isEmpty()
                || (ingress.getStatus().getLoadBalancer().getIngress().get(0).getIp() == null && ingress.getStatus().getLoadBalancer().getIngress().get(0).getHostname() == null)) {
            LOGGER.info("Ingress '{}' not ready yet", ingress.getMetadata().getName());
            return new NetworkResource("", false);
        }
        String host = ingress.getStatus().getLoadBalancer().getIngress().get(0).getIp();
        if (host == null) {
            host = ingress.getStatus().getLoadBalancer().getIngress().get(0).getHostname();
        }
        String endpoint = NetworkingConstants.HTTP_SCHEME + host + ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getPath().replace(PATH_REGEX, "");
        return new NetworkResource(endpoint, true);
    }
}
