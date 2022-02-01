package com.redhat.service.bridge.shard.operator.utils.networking;

import com.redhat.service.bridge.shard.operator.networking.OpenshiftNetworkingService;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.openshift.api.model.Ingress;
import io.fabric8.openshift.api.model.IngressBuilder;
import io.fabric8.openshift.api.model.IngressSpecBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteIngress;
import io.fabric8.openshift.api.model.RouteIngressBuilder;
import io.fabric8.openshift.api.model.RouteIngressConditionBuilder;
import io.fabric8.openshift.api.model.RouteSpec;
import io.fabric8.openshift.api.model.RouteSpecBuilder;
import io.fabric8.openshift.api.model.RouteStatus;
import io.fabric8.openshift.api.model.RouteStatusBuilder;
import io.fabric8.openshift.api.model.RouteTargetReference;
import io.fabric8.openshift.api.model.RouteTargetReferenceBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public class OpenshiftNetworkingTestUtils implements NetworkingTestUtils {

    private final OpenShiftClient client;

    public OpenshiftNetworkingTestUtils(OpenShiftClient client) {
        this.client = client;
        patchOpenshiftIngressDomain();
    }

    @Override
    public Namespaced getNetworkResource(String name, String namespace) {
        return client.routes().inNamespace(namespace).withName(name).get();
    }

    @Override
    public void patchNetworkResource(String name, String namespace) {
        Route route = client.routes().inNamespace(namespace).withName(name).get();

        RouteTargetReference routeTargetReference = new RouteTargetReferenceBuilder()
                .withName(name)
                .withKind("Service")
                .build();

        RouteSpec routeSpec = new RouteSpecBuilder()
                .withHost(NetworkingTestConstants.HOST_IP + "/" + name)
                .withTo(routeTargetReference)
                .build();

        route.setSpec(routeSpec);

        RouteIngress routeIngress = new RouteIngressBuilder()
                .withConditions(new RouteIngressConditionBuilder()
                        .withType("Admitted")
                        .build())
                .build();

        RouteStatus routeStatus = new RouteStatusBuilder()
                .withIngress(routeIngress)
                .build();

        route.setStatus(routeStatus);

        client.routes().inNamespace(namespace).createOrReplace(route);
    }

    private void patchOpenshiftIngressDomain() {
        Ingress openshiftIngress = new IngressBuilder()
                .withMetadata(new ObjectMetaBuilder().withName(OpenshiftNetworkingService.CLUSTER_DOMAIN_RESOURCE_NAME).build())
                .withSpec(new IngressSpecBuilder().withDomain("apps.openbridge-dev.fdvn.p1.openshiftapps.com").build())
                .build();
        this.client.config().ingresses().withName(OpenshiftNetworkingService.CLUSTER_DOMAIN_RESOURCE_NAME).create(openshiftIngress);
    }
}
