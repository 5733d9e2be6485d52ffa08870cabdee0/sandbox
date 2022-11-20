package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.networking.NetworkingConstants;
import com.redhat.service.smartevents.shard.operator.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.networking.v1.*;
import io.fabric8.kubernetes.client.KubernetesClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BridgeIngressServiceImpl implements BridgeIngressService {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    IstioGatewayProvider istioGatewayProvider;

    @Inject
    TemplateProvider templateProvider;

    @Override
    public Ingress createBridgeIngress(BridgeIngress bridgeIngress, String path) {
        Service istioGatewayService = istioGatewayProvider.getIstioGatewayService();
        return buildIngress(bridgeIngress, istioGatewayService, istioGatewayProvider.getIstioGatewayServicePort(), path);
    }

    private Ingress buildIngress(BridgeIngress bridgeIngress, Service istioGatewayService, Integer istioGatewayServicePort, String path) {
        /**
         * As the service might not be in the same namespace of the bridgeIngress (for example for the istio gateway) we can not set the owner references.
         */
        Ingress ingress = templateProvider.loadBridgeIngressKubernetesIngressTemplate(bridgeIngress, new TemplateImportConfig()
                .withNameFromParent()
                .withPrimaryResourceFromParent());
        // Inherit the namespace from the service
        ingress.getMetadata().setNamespace(istioGatewayService.getMetadata().getNamespace());

        IngressBackend ingressBackend = new IngressBackendBuilder()
                .withService(new IngressServiceBackendBuilder()
                        .withName(istioGatewayService.getMetadata().getName())
                        .withPort(new ServiceBackendPortBuilder().withNumber(istioGatewayServicePort).build())
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

    @Override
    public Ingress fetchBridgeIngress(BridgeIngress bridgeIngress) {
        Service service = istioGatewayProvider.getIstioGatewayService();
        return kubernetesClient.network().v1().ingresses()
                .inNamespace(service.getMetadata().getNamespace())
                .withName(bridgeIngress.getMetadata().getName())
                .get();
    }
}
