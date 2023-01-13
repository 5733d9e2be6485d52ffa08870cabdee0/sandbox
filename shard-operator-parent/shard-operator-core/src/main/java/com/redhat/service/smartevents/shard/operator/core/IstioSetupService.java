package com.redhat.service.smartevents.shard.operator.core;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.shard.operator.core.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.gateway.Gateway;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.requestauthentication.RequestAuthentication;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.virtualservice.VirtualService;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
@UnlessBuildProfile(value = "test") // We don't need to create bean for this service in test.
public class IstioSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IstioSetupService.class);

    protected static final String ISTIO_GATEWAY_NAMESPACE = "knative-eventing";
    protected static final String ISTIO_GATEWAY_NAME = "broker-gateway";
    protected static final String ISTIO_VIRTUAL_SERVICE_NAMESPACE = "knative-eventing";
    protected static final String ISTIO_VIRTUAL_SERVICE_NAME = "broker-virtual-service";
    protected static final String JWT_REQUEST_AUTHENTICATION_NAMESPACE = "istio-system";
    protected static final String JWT_REQUEST_AUTHENTICATION_NAME = "jwt-rh-sso";

    KubernetesClient kubernetesClient;

    TemplateProvider templateProvider;

    @ConfigProperty(name = "event-bridge.istio.jwt.issuer")
    String jwtIssuer;

    @ConfigProperty(name = "event-bridge.istio.jwt.jwksUri")
    String jwksUri;

    @Inject
    IstioSetupService(KubernetesClient kubernetesClient, TemplateProvider templateProvider) {
        this.kubernetesClient = kubernetesClient;
        this.templateProvider = templateProvider;
    }

    void setupIstioComponent(@Observes StartupEvent event) {

        createIstioGateway();

        createIstioVirtualService();

        createJWTRequestAuthentication();
    }

    protected void createIstioGateway() {

        Gateway existing = kubernetesClient.resources(Gateway.class)
                .inNamespace(ISTIO_GATEWAY_NAMESPACE)
                .withName(ISTIO_GATEWAY_NAME)
                .get();

        Gateway expected = templateProvider.loadIstioGatewayTemplate();
        expected.getMetadata().setName(ISTIO_GATEWAY_NAME);
        expected.getMetadata().setNamespace(ISTIO_GATEWAY_NAMESPACE);

        if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            try {
                kubernetesClient.resources(Gateway.class)
                        .inNamespace(ISTIO_GATEWAY_NAMESPACE)
                        .createOrReplace(expected);
            } catch (RuntimeException e) {
                LOGGER.error(
                        "Failed to create Istio Gateway resource. Please make sure it was properly deployed. The application keeps running due to https://issues.redhat.com/browse/MGDOBR-940 but the functionalitis are compromised.");
            }
        }
    }

    protected void createIstioVirtualService() {

        VirtualService existing = kubernetesClient.resources(VirtualService.class)
                .inNamespace(ISTIO_VIRTUAL_SERVICE_NAMESPACE)
                .withName(ISTIO_VIRTUAL_SERVICE_NAME)
                .get();
        VirtualService expected = templateProvider.loadIstioVirtualServiceTemplate();
        expected.getMetadata().setName(ISTIO_VIRTUAL_SERVICE_NAME);
        expected.getMetadata().setNamespace(ISTIO_VIRTUAL_SERVICE_NAMESPACE);
        expected.getSpec().getGateways().set(0, ISTIO_GATEWAY_NAME);

        if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            try {
                kubernetesClient.resources(VirtualService.class)
                        .inNamespace(ISTIO_VIRTUAL_SERVICE_NAMESPACE)
                        .createOrReplace(expected);
            } catch (RuntimeException e) {
                LOGGER.error(
                        "Failed to create Istio Virtual Service resource. Please make sure it was properly deployed. The application keeps running due to https://issues.redhat.com/browse/MGDOBR-940 but the functionalitis are compromised.");
            }
        }
    }

    protected void createJWTRequestAuthentication() {

        RequestAuthentication existing = kubernetesClient.resources(RequestAuthentication.class)
                .inNamespace(JWT_REQUEST_AUTHENTICATION_NAMESPACE)
                .withName(JWT_REQUEST_AUTHENTICATION_NAME)
                .get();
        RequestAuthentication expected = templateProvider.loadJWTRequestAuthenticationTemplate();
        expected.getMetadata().setName(JWT_REQUEST_AUTHENTICATION_NAME);
        expected.getMetadata().setNamespace(JWT_REQUEST_AUTHENTICATION_NAMESPACE);
        expected.getSpec().getJwtRules().get(0).setIssuer(jwtIssuer);
        expected.getSpec().getJwtRules().get(0).setJwksUri(jwksUri);

        if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            try {
                kubernetesClient.resources(RequestAuthentication.class)
                        .inNamespace(JWT_REQUEST_AUTHENTICATION_NAMESPACE)
                        .createOrReplace(expected);
            } catch (RuntimeException e) {
                LOGGER.error(
                        "Failed to create JWT Request Authentication resource. Please make sure it was properly deployed. The application keeps running due to https://issues.redhat.com/browse/MGDOBR-940 but the functionalities are compromised.");
            }
        }
    }
}
