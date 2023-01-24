package com.redhat.service.smartevents.shard.operator.v2.controllers;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.shard.operator.v2.TestSupport;
import com.redhat.service.smartevents.shard.operator.v2.providers.NamespaceProvider;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;
import com.redhat.service.smartevents.shard.operator.v2.resources.camel.CamelIntegration;
import com.redhat.service.smartevents.shard.operator.v2.utils.V2KubernetesResourcePatcher;
import com.redhat.service.smartevents.test.resource.KeycloakResource;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(value = KeycloakResource.class, restrictToAnnotatedClass = true)
class ManagedProcessorControllerTest {

    @Inject
    Operator operator;

    @Inject
    ManagedProcessorController managedProcessorController;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    NamespaceProvider namespaceProvider;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    V2KubernetesResourcePatcher kubernetesResourcePatcher;

    @BeforeEach
    public void beforeEach() {
        kubernetesResourcePatcher.cleanUp();
        operator.start();
    }

    @Test
    void operatorFirstReconcileRun() {
        // Given
        ManagedProcessor managedProcessor = buildManagedProcessor();

        // When
        UpdateControl<ManagedProcessor> updateControl = managedProcessorController.reconcile(managedProcessor, null);

        // Then
        assertThat(updateControl.isUpdateStatus()).isTrue();
        assertThat(managedProcessor.getStatus().isReady()).isFalse();

        CamelIntegration camelIntegration = kubernetesClient
                .resources(CamelIntegration.class)
                .inNamespace(managedProcessor.getMetadata().getNamespace())
                .withName(managedProcessor.getMetadata().getName())
                .get();

        assertThat(camelIntegration).isNotNull();
        assertThat(camelIntegration.getMetadata().getOwnerReferences().size()).isEqualTo(1);
        assertThat(camelIntegration.getMetadata().getLabels()).isNotNull();
    }

    private ManagedProcessor buildManagedProcessor() {
        return ManagedProcessor.fromBuilder()
                .withNamespace(namespaceProvider.getNamespaceName(TestSupport.BRIDGE_ID))
                .withProcessorId(TestSupport.PROCESSOR_ID)
                .withProcessorName(TestSupport.PROCESSOR_NAME)
                .withBridgeId(TestSupport.BRIDGE_ID)
                .withCustomerId(TestSupport.CUSTOMER_ID)
                .withDefinition(objectMapper.createObjectNode())
                .build();
    }
}
