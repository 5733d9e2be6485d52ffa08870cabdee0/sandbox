package com.redhat.service.smartevents.shard.operator.networking;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.shard.operator.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.utils.EventSourceFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkingService.class);
    private final KubernetesClient client;
    private final TemplateProvider templateProvider;
    private final IstioGatewayProvider istioGatewayProvider;

    public KubernetesNetworkingService(KubernetesClient client, TemplateProvider templateProvider, IstioGatewayProvider istioGatewayProvider) {
        this.client = client;
        this.templateProvider = templateProvider;
        this.istioGatewayProvider = istioGatewayProvider;
    }

    @Override
    public EventSource buildInformerEventSource(String component) {
        return EventSourceFactory.buildIngressesInformer(client, component);
    }

    @Override
    public NetworkResource fetchOrCreateBrokerNetworkIngress(BridgeIngress bridgeIngress, String path) {
        Service service = istioGatewayProvider.getIstioGatewayService();
        Ingress expected = buildIngress(bridgeIngress, service, istioGatewayProvider.getIstioGatewayServicePort(), path);

        Ingress existing = client.network().v1().ingresses()
                .inNamespace(service.getMetadata().getNamespace())
                .withName(bridgeIngress.getMetadata().getName())
                .get();

        if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            client.network().v1().ingresses()
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

    private Ingress buildIngress(BridgeIngress bridgeIngress, Service service, Integer port, String path) {
        /**
         * As the service might not be in the same namespace of the bridgeIngress (for example for the istio gateway) we can not set the owner references.
         */
        Ingress ingress = templateProvider.loadBridgeIngressKubernetesIngressTemplate(bridgeIngress, new TemplateImportConfig()
                .withNameFromParent()
                .withPrimaryResourceFromParent());
        // Inherit the namespace from the service
        ingress.getMetadata().setNamespace(service.getMetadata().getNamespace());

        IngressBackend ingressBackend = new IngressBackendBuilder()
                .withService(new IngressServiceBackendBuilder()
                        .withName(service.getMetadata().getName())
                        .withPort(new ServiceBackendPortBuilder().withNumber(port).build())
                        .build())
                .build();

        HTTPIngressPath httpIngressPath = new HTTPIngressPathBuilder()
                .withBackend(ingressBackend)
                .withPath(path)
                .withPathType(NetworkingConstants.K8S_INGRESS_PATH_TYPE)
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
            host = Optional.ofNullable(System.getenv("INGRESS_OVERRIDE_HOSTNAME")).orElse(ingress.getStatus().getLoadBalancer().getIngress().get(0).getHostname());
        }
        String endpoint = NetworkingConstants.HTTP_SCHEME + host + ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getPath();
        return new NetworkResource(endpoint, true);
    }
}
