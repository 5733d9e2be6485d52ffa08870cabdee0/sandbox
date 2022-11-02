package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RoutePortBuilder;
import io.fabric8.openshift.api.model.RouteTargetReferenceBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

import javax.inject.Inject;
import java.util.Base64;

public class BridgeRouteServiceImpl implements BridgeRouteService {

    @Inject
    OpenShiftClient openShiftClient;

    @Inject
    IstioGatewayProvider istioGatewayProvider;

    @Inject
    TemplateProvider templateProvider;

    @Override
    public Route createBridgeRoute(BridgeIngress bridgeIngress) {
        Service service = istioGatewayProvider.getIstioGatewayService();
        return buildRoute(bridgeIngress, service);
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
        route.getSpec().setHost(bridgeIngress.getSpec().getHost());

        route.getSpec().getTls()
                .setCertificate(bridgeIngress.getSpec().getDnsConfiguration().getTlsCertificate());
        route.getSpec().getTls()
                .setKey(bridgeIngress.getSpec().getDnsConfiguration().getTlsKey());

        route.getSpec().setTo(new RouteTargetReferenceBuilder()
                .withKind("Service")
                .withName(service.getMetadata().getName())
                .build());
        route.getSpec().setPort(new RoutePortBuilder().withTargetPort(new IntOrString("http2")).build());
        return route;
    }


    @Override
    public Route fetchBridgeRoute(BridgeIngress bridgeIngress) {
        Service service = istioGatewayProvider.getIstioGatewayService();
        return openShiftClient.routes()
                .inNamespace(service.getMetadata().getNamespace())
                .withName(bridgeIngress.getMetadata().getName())
                .get();
    }
}
