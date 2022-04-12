package com.redhat.service.smartevents.shard.operator.networking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.utils.EventSourceFactory;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteTargetReferenceBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

public class OpenshiftNetworkingService implements NetworkingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkingService.class);
    public static final String CLUSTER_DOMAIN_RESOURCE_NAME = "cluster";

    private final OpenShiftClient client;
    private final TemplateProvider templateProvider;

    public OpenshiftNetworkingService(OpenShiftClient client, TemplateProvider templateProvider) {
        this.client = client;
        this.templateProvider = templateProvider;
    }

    @Override
    public EventSource buildInformerEventSource(String component) {
        return EventSourceFactory.buildRoutesInformer(client, component);
    }

    @Override
    public NetworkResource fetchOrCreateNetworkIngress(BridgeIngress bridgeIngress, Service service) {
        Route expected = buildRoute(bridgeIngress, service);

        Route existing = client.routes().inNamespace(service.getMetadata().getNamespace()).withName(service.getMetadata().getName()).get();

        if (existing == null || !expected.getSpec().getTo().getName().equals(existing.getSpec().getTo().getName())) {
            client.routes().inNamespace(service.getMetadata().getNamespace()).createOrReplace(expected);
            return buildNetworkingResource(expected);
        }
        return buildNetworkingResource(existing);
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
        Route route = templateProvider.loadBridgeIngressOpenshiftRouteTemplate(bridgeIngress);

        // We have to provide the host manually in order not to exceed the 63 char limit in the dns label https://issues.redhat.com/browse/MGDOBR-271
        route.getSpec().setHost(String.format("%s.%s", bridgeIngress.getMetadata().getName(), getOpenshiftAppsDomain()));
        route.getSpec().setTo(new RouteTargetReferenceBuilder()
                .withKind("Service")
                .withName(service.getMetadata().getName())
                .build());
        return route;
    }

    // https://docs.openshift.com/container-platform/4.6/networking/routes/route-configuration.html
    private String getOpenshiftAppsDomain() {
        return client.config().ingresses().withName(CLUSTER_DOMAIN_RESOURCE_NAME).get().getSpec().getDomain();
    }

    private NetworkResource buildNetworkingResource(Route route) {
        if (route.getStatus() != null && "Admitted".equals(route.getStatus().getIngress().get(0).getConditions().get(0).getType())) {
            String endpoint = route.getSpec().getHost();
            endpoint = route.getSpec().getTls() != null ? NetworkingConstants.HTTPS_SCHEME + endpoint : NetworkingConstants.HTTP_SCHEME + endpoint;
            endpoint = endpoint + NetworkingConstants.EVENTS_ENDPOINT_SUFFIX;
            return new NetworkResource(endpoint, true);
        }

        LOGGER.info("Route '{}' not ready yet", route.getMetadata().getName());
        return new NetworkResource(null, false);
    }
}
