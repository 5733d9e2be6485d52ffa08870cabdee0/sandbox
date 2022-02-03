package com.redhat.service.bridge.shard.operator.resources;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.shard.operator.TestSupport;

import static com.redhat.service.bridge.shard.operator.resources.BridgeIngress.OB_RESOURCE_NAME_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

public class BridgeIngressTest {

    @Test
    public void fromDTO() {
        BridgeDTO dto = TestSupport.newRequestedBridgeDTO();
        BridgeIngress bridgeIngress = BridgeIngress.fromDTO(dto, "ns", "image");

        assertThat(bridgeIngress.getMetadata().getNamespace()).isEqualTo("ns");
        assertThat(bridgeIngress.getMetadata().getName()).isEqualTo(OB_RESOURCE_NAME_PREFIX + TestSupport.BRIDGE_ID);

        assertThat(bridgeIngress.getSpec().getBridgeName()).isEqualTo(dto.getName());
        assertThat(bridgeIngress.getSpec().getId()).isEqualTo(dto.getId());
        assertThat(bridgeIngress.getSpec().getImage()).isEqualTo("image");
        assertThat(bridgeIngress.getSpec().getCustomerId()).isEqualTo(dto.getCustomerId());
    }
}
