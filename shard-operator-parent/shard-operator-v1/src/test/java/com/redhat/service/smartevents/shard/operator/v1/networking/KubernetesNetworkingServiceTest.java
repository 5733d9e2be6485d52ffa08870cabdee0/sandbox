package com.redhat.service.smartevents.shard.operator.v1.networking;

import javax.inject.Inject;

import com.redhat.service.smartevents.shard.operator.v1.providers.TemplateProviderImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.shard.operator.core.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.v1.TestSupport;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeIngress;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

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
        Assertions.assertThat(networkResource.isReady()).isFalse();
        Assertions.assertThat(networkResource.getEndpoint()).isEqualTo("");
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
