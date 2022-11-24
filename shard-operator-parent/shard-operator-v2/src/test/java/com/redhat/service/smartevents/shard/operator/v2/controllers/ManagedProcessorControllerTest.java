package com.redhat.service.smartevents.shard.operator.v2.controllers;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.shard.operator.v2.TestSupport;
import com.redhat.service.smartevents.shard.operator.v2.providers.NamespaceProvider;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;
import com.redhat.service.smartevents.test.resource.KeycloakResource;

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
    ManagedProcessorController managedProcessorController;

    @Inject
    NamespaceProvider namespaceProvider;

    @Inject
    ObjectMapper objectMapper;

    @Test
    void testCreateNewManagedProcessor() {
        // Given
        ManagedProcessor managedProcessor = buildManagedProcessor();

        // When
        UpdateControl<ManagedProcessor> updateControl = managedProcessorController.reconcile(managedProcessor, null);

        // Then
        assertThat(updateControl.isUpdateStatus()).isFalse();
        assertThat(managedProcessor.getStatus().isReady()).isTrue();
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
