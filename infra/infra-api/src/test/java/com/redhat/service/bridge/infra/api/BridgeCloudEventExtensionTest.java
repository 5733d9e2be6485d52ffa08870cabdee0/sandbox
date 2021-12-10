package com.redhat.service.bridge.infra.api;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.cloudevents.CloudEventExtensions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class BridgeCloudEventExtensionTest {

    @Test
    public void readFrom() {

        String bridgeId = "myBridgeId";

        CloudEventExtensions extensions = Mockito.mock(CloudEventExtensions.class);
        when(extensions.getExtension(BridgeCloudEventExtension.BRIDGE_ID)).thenReturn(bridgeId);

        BridgeCloudEventExtension b = new BridgeCloudEventExtension();
        b.readFrom(extensions);
        assertThat(b.getBridgeId()).isEqualTo(bridgeId);
    }

    @Test
    public void getValue_bridgeId() {

        String bridgeId = "myBridgeId";

        BridgeCloudEventExtension b = new BridgeCloudEventExtension();
        b.setBridgeId(bridgeId);

        assertThat(b.getValue(BridgeCloudEventExtension.BRIDGE_ID)).isEqualTo(bridgeId);
    }
}
