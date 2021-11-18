package com.redhat.service.bridge.shard.operator.controllers;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.shard.operator.TestConstants;
import com.redhat.service.bridge.shard.operator.resources.BridgeExecutor;
import com.redhat.service.bridge.shard.operator.resources.BridgeExecutorSpec;
import com.redhat.service.bridge.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
public class BridgeExecutorControllerTest {

    @Inject
    BridgeExecutorController bridgeExecutorController;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    KubernetesResourcePatcher kubernetesResourcePatcher;

    @BeforeEach
    void setup() {
        kubernetesResourcePatcher.cleanUp();
    }

    @Test
    void testCreateNewBridgeIngress() {
        // Given
        BridgeExecutor bridgeExecutor = buildBridgeExecutor();

        // When
        UpdateControl<BridgeExecutor> updateControl = bridgeExecutorController.createOrUpdateResource(bridgeExecutor, null);

        // Then
        assertThat(updateControl.isUpdateStatusSubResource()).isTrue();
    }

    @Test
    void testBridgeIngressDeployment() {
        // Given
        BridgeExecutor bridgeExecutor = buildBridgeExecutor();

        // When
        bridgeExecutorController.createOrUpdateResource(bridgeExecutor, null);

        // Then
        Deployment deployment = kubernetesClient.apps().deployments().inNamespace(bridgeExecutor.getMetadata().getNamespace()).withName(bridgeExecutor.getMetadata().getName()).get();
        assertThat(deployment).isNotNull();
        assertThat(deployment.getMetadata().getOwnerReferences().size()).isEqualTo(1);
        assertThat(deployment.getMetadata().getLabels()).isNotNull();
        assertThat(deployment.getSpec().getSelector().getMatchLabels().size()).isEqualTo(1);
        assertThat(deployment.getSpec().getTemplate().getMetadata().getLabels()).isNotNull();
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage()).isNotNull();
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getName()).isNotNull();
    }

    private BridgeExecutor buildBridgeExecutor() {
        BridgeExecutorSpec bridgeExecutorSpec = new BridgeExecutorSpec();
        bridgeExecutorSpec.setId(TestConstants.PROCESSOR_ID);
        bridgeExecutorSpec.setProcessorName(TestConstants.PROCESSOR_NAME);
        bridgeExecutorSpec.setImage(TestConstants.EXECUTOR_IMAGE);
        bridgeExecutorSpec.setDefinition(null);
        bridgeExecutorSpec.setBridgeDTO(TestConstants.newAvailableBridgeDTO());

        BridgeExecutor bridgeExecutor = new BridgeExecutor();
        bridgeExecutor.setMetadata(
                new ObjectMetaBuilder()
                        .withName(BridgeExecutor.buildResourceName(TestConstants.PROCESSOR_ID))
                        .withNamespace(KubernetesResourceUtil.sanitizeName(TestConstants.CUSTOMER_ID))
                        .withLabels(new LabelsBuilder()
                                .withCustomerId(TestConstants.CUSTOMER_ID)
                                .withComponent(BridgeExecutor.COMPONENT_NAME)
                                .build())
                        .build());
        bridgeExecutor.setSpec(bridgeExecutorSpec);

        return bridgeExecutor;
    }
}
