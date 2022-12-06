package com.redhat.service.smartevents.shard.operator.v1.networking;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.shard.operator.core.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.core.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.v1.TestSupport;
import com.redhat.service.smartevents.shard.operator.v1.providers.TemplateProviderImpl;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeIngress;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
public class OpenshiftNetworkingServiceTest {

    @Inject
    OpenShiftClient client;

    @Inject
    IstioGatewayProvider istioGatewayProvider;

    @Test
    public void TestOpenshiftNetworkingService() {
        OpenshiftNetworkingService openshiftNetworkingService = new OpenshiftNetworkingService(client, new TemplateProviderImpl(), istioGatewayProvider);
        Secret secret = new Secret();
        Map<String, String> secretData = new HashMap<>();
        secretData.put(GlobalConfigurationsConstants.TLS_CERTIFICATE_SECRET, "VExTX0NFUlRJRklDQVRF");
        secretData.put(GlobalConfigurationsConstants.TLS_KEY_SECRET, "VExTX0tFWV9TRUNSRVQ=");
        secret.setData(secretData);

        NetworkResource networkResource = openshiftNetworkingService.fetchOrCreateBrokerNetworkIngress(buildRouteIngress(), secret, "testPath");
        assertThat(networkResource.isReady()).isFalse();
        assertThat(networkResource.getEndpoint()).isNull();
    }

    private BridgeIngress buildRouteIngress() {
        return BridgeIngress.fromBuilder()
                .withBridgeId(TestSupport.BRIDGE_ID)
                .withBridgeName(TestSupport.BRIDGE_NAME)
                .withCustomerId(TestSupport.CUSTOMER_ID)
                .withNamespace(KubernetesResourceUtil.sanitizeName(TestSupport.CUSTOMER_ID))
                .withHost(TestSupport.BRIDGE_HOST)
                .build();
    }
}
