package com.redhat.service.smartevents.shard.operator.networking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.shard.operator.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.utils.EventSourceFactory;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RoutePortBuilder;
import io.fabric8.openshift.api.model.RouteTargetReferenceBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

public class OpenshiftNetworkingService implements NetworkingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkingService.class);
    public static final String CLUSTER_DOMAIN_RESOURCE_NAME = "cluster";

    private final OpenShiftClient client;
    private final TemplateProvider templateProvider;
    private final IstioGatewayProvider istioGatewayProvider;

    public OpenshiftNetworkingService(OpenShiftClient client, TemplateProvider templateProvider, IstioGatewayProvider istioGatewayProvider) {
        this.client = client;
        this.templateProvider = templateProvider;
        this.istioGatewayProvider = istioGatewayProvider;
    }

    @Override
    public EventSource buildInformerEventSource(String component) {
        return EventSourceFactory.buildRoutesInformer(client, component);
    }

    @Override
    public NetworkResource fetchOrCreateBrokerNetworkIngress(BridgeIngress bridgeIngress, String path) {
        Service service = istioGatewayProvider.getIstioGatewayService();
        Route expected = buildRoute(bridgeIngress, service);

        Route existing = client.routes()
                .inNamespace(service.getMetadata().getNamespace())
                .withName(bridgeIngress.getMetadata().getName())
                .get();

        if (existing == null || !expected.getSpec().getTo().getName().equals(existing.getSpec().getTo().getName())) {
            client.routes()
                    .inNamespace(service.getMetadata().getNamespace())
                    .withName(bridgeIngress.getMetadata().getName())
                    .createOrReplace(expected);
            return buildNetworkingResource(expected, path);
        }
        return buildNetworkingResource(existing, path);
    }

    @Override
    public boolean delete(String name, String namespace) {
        try {
            return client.routes().inNamespace(namespace).withName(name).delete();
        } catch (Exception e) {
            LOGGER.debug("Can't delete ingress with name '{}' because it does not exist", name);
            return false;
        }
    }

    private Route buildRoute(BridgeIngress bridgeIngress, Service service) {
        /**
         * As the service might not be in the same namespace of the bridgeIngress (for example for the istio gateway) we can not set the owner references.
         */
        Route route = templateProvider.loadBridgeIngressOpenshiftRouteTemplate(bridgeIngress,
                new TemplateImportConfig()
                        .withNameFromParent()
                        .withPrimaryResourceFromParent());
        // Inherit namespace from service and not from bridgeIngress
        route.getMetadata().setNamespace(service.getMetadata().getNamespace());

        // We have to provide the host manually in order not to exceed the 63 char limit in the dns label https://issues.redhat.com/browse/MGDOBR-271
        route.getSpec().setHost(String.format("%s.%s", bridgeIngress.getMetadata().getName(), getOpenshiftAppsDomain()));
        route.getSpec().setTo(new RouteTargetReferenceBuilder()
                .withKind("Service")
                .withName(service.getMetadata().getName())
                .build());
        route.getSpec().setPort(new RoutePortBuilder().withTargetPort(new IntOrString("http2")).build());
        return route;
    }

    // https://docs.openshift.com/container-platform/4.6/networking/routes/route-configuration.html
    private String getOpenshiftAppsDomain() {
        return client.config().ingresses().withName(CLUSTER_DOMAIN_RESOURCE_NAME).get().getSpec().getDomain();
    }

    private NetworkResource buildNetworkingResource(Route route, String path) {
        if (route.getStatus() != null && "Admitted".equals(route.getStatus().getIngress().get(0).getConditions().get(0).getType())) {
            String endpoint = route.getSpec().getHost();
            endpoint = route.getSpec().getTls() != null ? NetworkingConstants.HTTPS_SCHEME + endpoint : NetworkingConstants.HTTP_SCHEME + endpoint;
            endpoint = endpoint + path;
            return new NetworkResource(endpoint, true);
        }

        LOGGER.info("Route '{}' not ready yet", route.getMetadata().getName());
        return new NetworkResource(null, false);
    }
}
