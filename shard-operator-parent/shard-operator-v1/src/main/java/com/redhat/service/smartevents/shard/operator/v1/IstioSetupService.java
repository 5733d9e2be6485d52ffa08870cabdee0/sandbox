package com.redhat.service.smartevents.shard.operator.v1;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.shard.operator.core.resources.istio.gateway.Gateway;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.requestauthentication.RequestAuthentication;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.virtualservice.VirtualService;
import com.redhat.service.smartevents.shard.operator.v1.providers.TemplateProvider;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class IstioSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IstioSetupService.class);

    private static final String ISTIO_GATEWAY_NAMESPACE = "knative-eventing";
    private static final String ISTIO_GATEWAY_NAME = "broker-gateway";
    private static final String ISTIO_VIRTUAL_SERVICE_NAMESPACE = "knative-eventing";
    private static final String ISTIO_VIRTUAL_SERVICE_NAME = "broker-virtual-service";
    private static final String JWT_REQUEST_AUTHENTICATION_NAMESPACE = "istio-system";
    private static final String JWT_REQUEST_AUTHENTICATION_NAME = "jwt-rh-sso";

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TemplateProvider templateProvider;

    @ConfigProperty(name = "event-bridge.istio.jwt.issuer")
    String jwtIssuer;

    @ConfigProperty(name = "event-bridge.istio.jwt.jwksUri")
    String jwksUri;

    void setupIstioComponent(@Observes StartupEvent event) {

        createIstioGateway();

        createIstioVirtualService();

        createJWTRequestAuthentication();
    }

    private void createIstioGateway() {

        Gateway existing = kubernetesClient.resources(Gateway.class)
                .inNamespace(ISTIO_GATEWAY_NAMESPACE)
                .withName(ISTIO_GATEWAY_NAME)
                .get();

        if (existing == null) {
            Gateway expected = templateProvider.loadIstioGatewayTemplate();
            expected.getMetadata().setName(ISTIO_GATEWAY_NAME);
            expected.getMetadata().setNamespace(ISTIO_GATEWAY_NAMESPACE);

            try {
                kubernetesClient.resources(Gateway.class)
                        .inNamespace(ISTIO_GATEWAY_NAMESPACE)
                        .create(expected);
            } catch (RuntimeException e) {
                LOGGER.error(
                        "Failed to create Istio Gateway resource. Please make sure it was properly deployed. The application keeps running due to https://issues.redhat.com/browse/MGDOBR-940 but the functionalitis are compromised.");
            }
        }
    }

    private void createIstioVirtualService() {

        VirtualService existing = kubernetesClient.resources(VirtualService.class)
                .inNamespace(ISTIO_VIRTUAL_SERVICE_NAMESPACE)
                .withName(ISTIO_VIRTUAL_SERVICE_NAME)
                .get();

        if (existing == null) {
            VirtualService expected = templateProvider.loadIstioVirtualServiceTemplate();
            expected.getMetadata().setName(ISTIO_VIRTUAL_SERVICE_NAME);
            expected.getMetadata().setNamespace(ISTIO_VIRTUAL_SERVICE_NAMESPACE);
            expected.getSpec().getGateways().set(0, ISTIO_GATEWAY_NAME);
            try {
                kubernetesClient.resources(VirtualService.class)
                        .inNamespace(ISTIO_VIRTUAL_SERVICE_NAMESPACE)
                        .create(expected);
            } catch (RuntimeException e) {
                LOGGER.error(
                        "Failed to create Istio Virtual Service resource. Please make sure it was properly deployed. The application keeps running due to https://issues.redhat.com/browse/MGDOBR-940 but the functionalitis are compromised.");
            }
        }
    }

    private void createJWTRequestAuthentication() {

        RequestAuthentication existing = kubernetesClient.resources(RequestAuthentication.class)
                .inNamespace(JWT_REQUEST_AUTHENTICATION_NAMESPACE)
                .withName(JWT_REQUEST_AUTHENTICATION_NAME)
                .get();

        if (existing == null) {
            RequestAuthentication expected = templateProvider.loadJWTRequestAuthenticationTemplate();
            expected.getMetadata().setName(JWT_REQUEST_AUTHENTICATION_NAME);
            expected.getMetadata().setNamespace(JWT_REQUEST_AUTHENTICATION_NAMESPACE);
            expected.getSpec().getJwtRules().get(0).setIssuer(jwtIssuer);
            expected.getSpec().getJwtRules().get(0).setJwksUri(jwksUri);
            try {
                kubernetesClient.resources(RequestAuthentication.class)
                        .inNamespace(JWT_REQUEST_AUTHENTICATION_NAMESPACE)
                        .create(expected);
            } catch (RuntimeException e) {
                LOGGER.error(
                        "Failed to create JWT Request Authentication resource. Please make sure it was properly deployed. The application keeps running due to https://issues.redhat.com/browse/MGDOBR-940 but the functionalitis are compromised.");
            }
        }
    }
}
