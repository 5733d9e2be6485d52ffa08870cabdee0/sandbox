package com.redhat.service.bridge.shard.operator.monitoring;

import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.shard.operator.BridgeIngressService;
import com.redhat.service.bridge.shard.operator.TestSupport;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.utils.KubernetesResourcePatcher;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

import static com.redhat.service.bridge.shard.operator.monitoring.ServiceMonitorClient.SERVICE_MONITOR_CRD_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithKubernetesTestServer()
public class ServiceMonitorServiceTest {

    @Inject
    ServiceMonitorService serviceMonitorService;

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    KubernetesResourcePatcher kubernetesResourcePatcher;

    @Inject
    KubernetesClient kubernetesClient;

    @BeforeEach
    void setup() {
        kubernetesResourcePatcher.cleanUp();
    }

    @Test
    void fetchOrCreateServiceMonitor() {
        // Given
        this.registerServiceMonitor();
        final BridgeDTO bridge = TestSupport.newAvailableBridgeDTO();
        final BridgeIngress bridgeIngress = BridgeIngress.fromDTO(bridge, "default", TestSupport.INGRESS_IMAGE);
        final Deployment deployment = bridgeIngressService.fetchOrCreateBridgeIngressDeployment(bridgeIngress);
        final Service service = bridgeIngressService.fetchOrCreateBridgeIngressService(bridgeIngress, deployment);

        // When
        final Optional<ServiceMonitor> serviceMonitor = serviceMonitorService.fetchOrCreateServiceMonitor(bridgeIngress, service);

        // Then
        assertThat(serviceMonitor).isPresent();
    }

    private void registerServiceMonitor() {
        final CustomResourceDefinition serviceMonitorCRD =
                kubernetesClient.apiextensions().v1().customResourceDefinitions().load(this.getClass().getResourceAsStream("/k8s/servicemonitor.v1.crd.yaml")).get();
        kubernetesClient.apiextensions().v1().customResourceDefinitions().withName(SERVICE_MONITOR_CRD_NAME).create(serviceMonitorCRD);
    }
}
