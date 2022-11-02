package com.redhat.service.smartevents.shard.operator.resources;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v1.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.TestSupport;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;

import static com.redhat.service.smartevents.shard.operator.resources.BridgeIngress.OB_RESOURCE_NAME_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

public class BridgeIngressTest {

    @Test
    public void fromDTO() {
        BridgeDTO dto = TestSupport.newRequestedBridgeDTO();
        BridgeIngress bridgeIngress = BridgeIngress.fromDTO(dto, "ns");

        assertThat(bridgeIngress.getMetadata().getNamespace()).isEqualTo("ns");
        assertThat(bridgeIngress.getMetadata().getName()).isEqualTo(OB_RESOURCE_NAME_PREFIX + TestSupport.BRIDGE_ID);
        assertThat(bridgeIngress.getMetadata().getLabels().get(LabelsBuilder.CREATED_BY_LABEL)).isEqualTo(LabelsBuilder.OPERATOR_NAME);
        assertThat(bridgeIngress.getMetadata().getLabels().get(LabelsBuilder.MANAGED_BY_LABEL)).isEqualTo(LabelsBuilder.OPERATOR_NAME);
        assertThat(bridgeIngress.getMetadata().getLabels().get(LabelsBuilder.COMPONENT_LABEL)).isEqualTo(BridgeIngress.COMPONENT_NAME);

        assertThat(bridgeIngress.getSpec().getBridgeName()).isEqualTo(dto.getName());
        assertThat(bridgeIngress.getSpec().getId()).isEqualTo(dto.getId());
        assertThat(bridgeIngress.getSpec().getCustomerId()).isEqualTo(dto.getCustomerId());
        assertThat(bridgeIngress.getSpec().getOwner()).isEqualTo(dto.getOwner());
    }
}
