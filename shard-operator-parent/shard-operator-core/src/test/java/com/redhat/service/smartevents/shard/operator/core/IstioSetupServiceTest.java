package com.redhat.service.smartevents.shard.operator.core;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.shard.operator.core.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.gateway.Gateway;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.requestauthentication.RequestAuthentication;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.virtualservice.VirtualService;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static com.redhat.service.smartevents.shard.operator.core.IstioSetupService.ISTIO_GATEWAY_NAME;
import static com.redhat.service.smartevents.shard.operator.core.IstioSetupService.ISTIO_GATEWAY_NAMESPACE;
import static com.redhat.service.smartevents.shard.operator.core.IstioSetupService.ISTIO_VIRTUAL_SERVICE_NAME;
import static com.redhat.service.smartevents.shard.operator.core.IstioSetupService.ISTIO_VIRTUAL_SERVICE_NAMESPACE;
import static com.redhat.service.smartevents.shard.operator.core.IstioSetupService.JWT_REQUEST_AUTHENTICATION_NAME;
import static com.redhat.service.smartevents.shard.operator.core.IstioSetupService.JWT_REQUEST_AUTHENTICATION_NAMESPACE;

@QuarkusTest
@WithOpenShiftTestServer
public class IstioSetupServiceTest {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TemplateProvider templateProvider;

    @Test
    public void testSetupIstioComponent() {
        // test
        IstioSetupService istioSetupService = new IstioSetupService(kubernetesClient, templateProvider);
        istioSetupService.setupIstioComponent(new StartupEvent());

        // assert
        Gateway deployedGateway = kubernetesClient.resources(Gateway.class)
                .inNamespace(ISTIO_GATEWAY_NAMESPACE)
                .withName(ISTIO_GATEWAY_NAME)
                .get();
        Assertions.assertThat(deployedGateway).isNotNull();

        VirtualService deployedVirtualService = kubernetesClient.resources(VirtualService.class)
                .inNamespace(ISTIO_VIRTUAL_SERVICE_NAMESPACE)
                .withName(ISTIO_VIRTUAL_SERVICE_NAME)
                .get();
        Assertions.assertThat(deployedVirtualService).isNotNull();

        RequestAuthentication deployedRequestAuthentication = kubernetesClient.resources(RequestAuthentication.class)
                .inNamespace(JWT_REQUEST_AUTHENTICATION_NAMESPACE)
                .withName(JWT_REQUEST_AUTHENTICATION_NAME)
                .get();
        Assertions.assertThat(deployedRequestAuthentication).isNotNull();
    }

    @Test
    public void testGatewayModificationApplied() {
        // setup
        Gateway expected = templateProvider.loadIstioGatewayTemplate();
        expected.getMetadata().setName(ISTIO_GATEWAY_NAME);
        expected.getMetadata().setNamespace(ISTIO_GATEWAY_NAMESPACE);
        expected.getSpec().getSelector().setIstio("test"); // modify istio selector name
        kubernetesClient.resources(Gateway.class)
                .inNamespace(ISTIO_GATEWAY_NAMESPACE)
                .createOrReplace(expected);

        // test
        IstioSetupService istioSetupService = new IstioSetupService(kubernetesClient, templateProvider);
        istioSetupService.createIstioGateway();

        // assert
        Gateway deployedGateway = kubernetesClient.resources(Gateway.class)
                .inNamespace(ISTIO_GATEWAY_NAMESPACE)
                .withName(ISTIO_GATEWAY_NAME)
                .get();
        Assertions.assertThat(deployedGateway).isNotNull();
        Assertions.assertThat(deployedGateway.getSpec().getSelector().getIstio()).isEqualTo("rhose-ingressgateway");
    }

    @Test
    public void testVirtualServiceModificationApplied() {
        // setup
        VirtualService expected = templateProvider.loadIstioVirtualServiceTemplate();
        expected.getMetadata().setName(ISTIO_VIRTUAL_SERVICE_NAME);
        expected.getMetadata().setNamespace(ISTIO_VIRTUAL_SERVICE_NAMESPACE);
        expected.getSpec().getGateways().set(0, "test_gateway"); // modify gateway name
        kubernetesClient.resources(VirtualService.class)
                .inNamespace(ISTIO_VIRTUAL_SERVICE_NAMESPACE)
                .createOrReplace(expected);
        // test
        IstioSetupService istioSetupService = new IstioSetupService(kubernetesClient, templateProvider);
        istioSetupService.createIstioVirtualService();

        // assert
        VirtualService deployedVirtualService = kubernetesClient.resources(VirtualService.class)
                .inNamespace(ISTIO_VIRTUAL_SERVICE_NAMESPACE)
                .withName(ISTIO_VIRTUAL_SERVICE_NAME)
                .get();
        Assertions.assertThat(deployedVirtualService).isNotNull();
        Assertions.assertThat(deployedVirtualService.getSpec().getGateways().get(0)).isEqualTo(ISTIO_GATEWAY_NAME);
    }

    @Test
    public void testRequestAuthenticationModificationApplied() {
        // setup
        RequestAuthentication expected = templateProvider.loadJWTRequestAuthenticationTemplate();
        expected.getMetadata().setName(JWT_REQUEST_AUTHENTICATION_NAME);
        expected.getMetadata().setNamespace(JWT_REQUEST_AUTHENTICATION_NAMESPACE);
        expected.getSpec().getJwtRules().get(0).setIssuer("test_jwtIssuer");
        expected.getSpec().getJwtRules().get(0).setJwksUri("test_jwksUri");
        kubernetesClient.resources(RequestAuthentication.class)
                .inNamespace(JWT_REQUEST_AUTHENTICATION_NAMESPACE)
                .createOrReplace(expected);

        // test
        IstioSetupService istioSetupService = new IstioSetupService(kubernetesClient, templateProvider);
        istioSetupService.createJWTRequestAuthentication();

        // assert
        RequestAuthentication deployedRequestAuthentication = kubernetesClient.resources(RequestAuthentication.class)
                .inNamespace(JWT_REQUEST_AUTHENTICATION_NAMESPACE)
                .withName(JWT_REQUEST_AUTHENTICATION_NAME)
                .get();
        Assertions.assertThat(deployedRequestAuthentication).isNotNull();
        Assertions.assertThat(deployedRequestAuthentication.getSpec().getJwtRules().get(0).getIssuer()).isNotEqualTo("test_jwtIssuer");
        Assertions.assertThat(deployedRequestAuthentication.getSpec().getJwtRules().get(0).getJwksUri()).isNotEqualTo("test_jwksUri");
    }
}
