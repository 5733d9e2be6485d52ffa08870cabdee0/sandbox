package com.redhat.service.smartevents.shard.operator.v1.networking;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.shard.operator.core.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.v1.TestSupport;
import com.redhat.service.smartevents.shard.operator.v1.providers.TemplateProviderImpl;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeIngress;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
public class KubernetesNetworkingServiceTest {

    @Inject
    KubernetesClient client;

    @Inject
    IstioGatewayProvider istioGatewayProvider;

    @Test
    public void TestFetchOrCreateBrokerNetworkIngress() {
        KubernetesNetworkingService kubernetesNetworkingService = new KubernetesNetworkingService(client, new TemplateProviderImpl(), istioGatewayProvider);
        NetworkResource networkResource = kubernetesNetworkingService.fetchOrCreateBrokerNetworkIngress(buildBridgeIngress(), null, "testPath");
        assertThat(networkResource.isReady()).isFalse();
        assertThat(networkResource.getEndpoint()).isEqualTo("");
    }

    private BridgeIngress buildBridgeIngress() {
        return BridgeIngress.fromBuilder()
                .withBridgeId(TestSupport.BRIDGE_ID)
                .withBridgeName(TestSupport.BRIDGE_NAME)
                .withCustomerId(TestSupport.CUSTOMER_ID)
                .withNamespace(KubernetesResourceUtil.sanitizeName(TestSupport.CUSTOMER_ID))
                .withHost(TestSupport.BRIDGE_HOST)
                .build();
    }
}
