package com.redhat.service.bridge.shard.operator.networking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;
import com.redhat.service.bridge.shard.operator.watchers.networking.OpenshiftRouteEventSource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.api.model.RouteSpec;
import io.fabric8.openshift.api.model.RouteSpecBuilder;
import io.fabric8.openshift.api.model.RouteTargetReferenceBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;

public class OpenshiftNetworkingService implements NetworkingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkingService.class);

    private final OpenShiftClient client;

    public OpenshiftNetworkingService(OpenShiftClient client) {
        this.client = client;
    }

    @Override
    public AbstractEventSource createAndRegisterWatchNetworkResource(String component) {
        return OpenshiftRouteEventSource.createAndRegisterWatch(client, component);
    }

    @Override
    public NetworkResource fetchOrCreateNetworkIngress(Service service) {
        Route route = client.routes().inNamespace(service.getMetadata().getNamespace()).withName(service.getMetadata().getName()).get();

        if (route == null) {
            route = buildRoute(service);
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

    private Route buildRoute(Service service) {
        ObjectMeta metadata = new ObjectMetaBuilder()
                .withOwnerReferences(service.getMetadata().getOwnerReferences())
                .withLabels(
                        new LabelsBuilder()
                                .withComponent(service.getMetadata().getLabels().get(LabelsBuilder.COMPONENT_LABEL))
                                .buildWithDefaults())
                .withName(service.getMetadata().getName())
                .build();

        RouteSpec routeSpec = new RouteSpecBuilder()
                .withTo(new RouteTargetReferenceBuilder()
                        .withKind("Service")
                        .withName(service.getMetadata().getName())
                        .build())
                .build();

        Route route = new RouteBuilder()
                .withMetadata(metadata)
                .withSpec(routeSpec)
                .build();

        return route;
    }

    private NetworkResource buildNetworkingResource(Route route) {
        if ("Admitted".equals(route.getStatus().getIngress().get(0).getConditions().get(0).getType())) {
            String endpoint = route.getSpec().getHost();
            endpoint = route.getSpec().getTls() != null ? NetworkingConstants.HTTPS_SCHEME + endpoint : NetworkingConstants.HTTP_SCHEME + endpoint;
            return new NetworkResource(endpoint, true);
        }

        LOGGER.info("Route {} not ready yet", route.getMetadata().getName());
        return new NetworkResource(null, false);
    }
}
