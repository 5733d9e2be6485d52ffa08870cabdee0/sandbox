package com.redhat.service.bridge.shard.operator.networking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.shard.operator.providers.TemplateProvider;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.watchers.networking.OpenshiftRouteEventSource;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteSpec;
import io.fabric8.openshift.api.model.RouteSpecBuilder;
import io.fabric8.openshift.api.model.RouteTargetReferenceBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;

public class OpenshiftNetworkingService implements NetworkingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkingService.class);

    private final OpenShiftClient client;
    private final TemplateProvider templateProvider;

    public OpenshiftNetworkingService(OpenShiftClient client, TemplateProvider templateProvider) {
        this.client = client;
        this.templateProvider = templateProvider;
    }

    @Override
    public AbstractEventSource createAndRegisterWatchNetworkResource(String component) {
        return OpenshiftRouteEventSource.createAndRegisterWatch(client, component);
    }

    // TODO: if the retrieved resource spec is not equal to the expected one, we should redeploy https://issues.redhat.com/browse/MGDOBR-140
    @Override
    public NetworkResource fetchOrCreateNetworkIngress(BridgeIngress bridgeIngress, Service service) {
        Route route = client.routes().inNamespace(service.getMetadata().getNamespace()).withName(service.getMetadata().getName()).get();

        if (route == null) {
            route = buildRoute(bridgeIngress, service);
            client.routes().inNamespace(service.getMetadata().getNamespace()).create(route);
        }
        return buildNetworkingResource(route);
    }

    @Override
    public boolean delete(String name, String namespace) {
        try {
            return client.routes().inNamespace(namespace).withName(name).delete();
        } catch (Exception e) {
            LOGGER.debug("Can't delete ingress with name {} because it does not exist", name);
            return false;
        }
    }

    private Route buildRoute(BridgeIngress bridgeIngress, Service service) {
        Route route = templateProvider.loadBridgeOpenshiftRouteTemplate(bridgeIngress);

        RouteSpec routeSpec = new RouteSpecBuilder()
                .withTo(new RouteTargetReferenceBuilder()
                        .withKind("Service")
                        .withName(service.getMetadata().getName())
                        .build())
                .build();

        route.setSpec(routeSpec);

        return route;
    }

    private NetworkResource buildNetworkingResource(Route route) {
        if (route.getStatus() != null && "Admitted".equals(route.getStatus().getIngress().get(0).getConditions().get(0).getType())) {
            String endpoint = route.getSpec().getHost();
            endpoint = route.getSpec().getTls() != null ? NetworkingConstants.HTTPS_SCHEME + endpoint : NetworkingConstants.HTTP_SCHEME + endpoint;
            return new NetworkResource(endpoint, true);
        }

        LOGGER.info("Route {} not ready yet", route.getMetadata().getName());
        return new NetworkResource(null, false);
    }
}
