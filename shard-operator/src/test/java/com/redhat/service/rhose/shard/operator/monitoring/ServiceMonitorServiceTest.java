package com.redhat.service.rhose.shard.operator.monitoring;

import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.rhose.infra.models.dto.BridgeDTO;
import com.redhat.service.rhose.shard.operator.BridgeIngressService;
import com.redhat.service.rhose.shard.operator.TestSupport;
import com.redhat.service.rhose.shard.operator.WithPrometheus;
import com.redhat.service.rhose.shard.operator.resources.BridgeIngress;
import com.redhat.service.rhose.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.rhose.shard.operator.utils.LabelsBuilder;
import com.redhat.service.rhose.test.resource.KeycloakResource;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithKubernetesTestServer
@QuarkusTestResource(value = KeycloakResource.class, restrictToAnnotatedClass = true)
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
        final Secret secretMock = new SecretBuilder().withMetadata(new ObjectMetaBuilder().withName(bridgeIngress.getMetadata().getName()).build()).build();
        final Deployment deployment = bridgeIngressService.fetchOrCreateBridgeIngressDeployment(bridgeIngress, secretMock);
        final Service service = bridgeIngressService.fetchOrCreateBridgeIngressService(bridgeIngress, deployment);

        // When
        final Optional<ServiceMonitor> serviceMonitor = serviceMonitorService.fetchOrCreateServiceMonitor(bridgeIngress, service, "ingress");

        // Then
        assertThat(serviceMonitor).isPresent();
        // check: https://prometheus-operator.dev/docs/operator/troubleshooting/#overview-of-servicemonitor-tagging-and-related-elements
        assertThat(serviceMonitor.get().getSpec().getSelector().getMatchLabels()).containsEntry(LabelsBuilder.INSTANCE_LABEL, deployment.getMetadata().getName());
        assertThat(serviceMonitor.get().getMetadata().getLabels()).containsEntry(LabelsBuilder.INSTANCE_LABEL, deployment.getMetadata().getName());
        assertThat(serviceMonitor.get().getMetadata().getLabels()).containsEntry(LabelsBuilder.MANAGED_BY_LABEL, LabelsBuilder.OPERATOR_NAME);
        assertThat(serviceMonitor.get().getMetadata().getLabels()).containsEntry(LabelsBuilder.CREATED_BY_LABEL, LabelsBuilder.OPERATOR_NAME);
        assertThat(serviceMonitor.get().getMetadata().getLabels()).containsEntry(LabelsBuilder.COMPONENT_LABEL, "ingress");
        assertThat(service.getMetadata().getLabels()).containsEntry(LabelsBuilder.INSTANCE_LABEL, deployment.getMetadata().getName());
    }
}
