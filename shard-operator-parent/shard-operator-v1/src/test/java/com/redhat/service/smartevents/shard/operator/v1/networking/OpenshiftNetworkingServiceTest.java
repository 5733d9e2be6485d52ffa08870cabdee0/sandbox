package com.redhat.service.smartevents.shard.operator.v1.networking;

import com.redhat.service.smartevents.shard.operator.core.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.core.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.v1.TestSupport;
import com.redhat.service.smartevents.shard.operator.v1.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeIngress;
//import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
public class OpenshiftNetworkingServiceTest {

    @Inject
    OpenShiftClient client;

    @Inject
    TemplateProvider templateProvider;

    @Inject
    IstioGatewayProvider istioGatewayProvider;

    @Test
    public void TestOpenshiftNetworkingService() {
        OpenshiftNetworkingService openshiftNetworkingService = new OpenshiftNetworkingService(client, templateProvider, istioGatewayProvider);
        Secret secret=new Secret();
        Map<String, String> Secretdata=new HashMap<>();
        Secretdata.put(GlobalConfigurationsConstants.TLS_CERTIFICATE_SECRET,"VExTX0NFUlRJRklDQVRF");
        Secretdata.put(GlobalConfigurationsConstants.TLS_KEY_SECRET,"VExTX0tFWV9TRUNSRVQ=");
        secret.setData(Secretdata);


       NetworkResource networkResource = openshiftNetworkingService.fetchOrCreateBrokerNetworkIngress(buildRouteIngress(), secret, "testPath");
        Assertions.assertThat(networkResource.isReady()).isFalse();
        Assertions.assertThat(networkResource.getEndpoint()).isNull();
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