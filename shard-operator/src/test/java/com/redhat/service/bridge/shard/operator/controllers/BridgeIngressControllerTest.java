package com.redhat.service.bridge.shard.operator.controllers;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.shard.operator.TestConstants;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngressSpec;
import com.redhat.service.bridge.shard.operator.utils.RFC1123Sanitizer;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class BridgeIngressControllerTest {

    @Inject
    BridgeIngressController bridgeIngressController;

    @Test
    void testCreateNewVersion() {
        //Given
        BridgeIngressSpec bridgeIngressSpec = new BridgeIngressSpec();
        bridgeIngressSpec.setId(TestConstants.BRIDGE_ID);
        bridgeIngressSpec.setBridgeName(TestConstants.BRIDGE_NAME);
        bridgeIngressSpec.setImage(TestConstants.INGRESS_IMAGE);
        bridgeIngressSpec.setCustomerId(TestConstants.CUSTOMER_ID);

        BridgeIngress bridgeIngress = new BridgeIngress();
        bridgeIngress.setMetadata(
                new ObjectMetaBuilder()
                        .withName(RFC1123Sanitizer.sanitize(TestConstants.BRIDGE_ID))
                        .withNamespace(RFC1123Sanitizer.sanitize(TestConstants.CUSTOMER_ID))
                        .build());
        bridgeIngress.setSpec(bridgeIngressSpec);

        //When
        UpdateControl<BridgeIngress> updateControl = bridgeIngressController.createOrUpdateResource(bridgeIngress, null);

        //Then
        assertThat(updateControl.isUpdateStatusSubResource()).isTrue();
    }
}
