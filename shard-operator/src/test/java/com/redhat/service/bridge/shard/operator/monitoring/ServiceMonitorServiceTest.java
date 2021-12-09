package com.redhat.service.bridge.shard.operator.monitoring;

import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.shard.operator.BridgeIngressService;
import com.redhat.service.bridge.shard.operator.TestSupport;
import com.redhat.service.bridge.shard.operator.WithPrometheus;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

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
    @WithPrometheus
    void fetchOrCreateServiceMonitor() {
        // Given
        final BridgeDTO bridge = TestSupport.newAvailableBridgeDTO();
        final BridgeIngress bridgeIngress = BridgeIngress.fromDTO(bridge, "default", TestSupport.INGRESS_IMAGE);
        final Deployment deployment = bridgeIngressService.fetchOrCreateBridgeIngressDeployment(bridgeIngress);
        final Service service = bridgeIngressService.fetchOrCreateBridgeIngressService(bridgeIngress, deployment);

        // When
        final Optional<ServiceMonitor> serviceMonitor = serviceMonitorService.fetchOrCreateServiceMonitor(bridgeIngress, service);

        // Then
        assertThat(serviceMonitor).isPresent();
        // check: https://prometheus-operator.dev/docs/operator/troubleshooting/#overview-of-servicemonitor-tagging-and-related-elements
        assertThat(serviceMonitor.get().getSpec().getSelector().getMatchLabels()).containsEntry(LabelsBuilder.INSTANCE_LABEL, deployment.getMetadata().getName());
        assertThat(serviceMonitor.get().getMetadata().getLabels()).containsEntry(LabelsBuilder.INSTANCE_LABEL, deployment.getMetadata().getName());
        assertThat(serviceMonitor.get().getMetadata().getLabels()).containsEntry(LabelsBuilder.MANAGED_BY_LABEL, LabelsBuilder.OPERATOR_NAME);
        assertThat(serviceMonitor.get().getMetadata().getLabels()).containsEntry(LabelsBuilder.CREATED_BY_LABEL, LabelsBuilder.OPERATOR_NAME);
        assertThat(service.getMetadata().getLabels()).containsEntry(LabelsBuilder.INSTANCE_LABEL, deployment.getMetadata().getName());
    }
}
