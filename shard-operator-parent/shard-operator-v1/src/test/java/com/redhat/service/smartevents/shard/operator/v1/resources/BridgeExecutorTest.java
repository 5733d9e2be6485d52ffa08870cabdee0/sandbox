package com.redhat.service.smartevents.shard.operator.v1.resources;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v1.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v1.TestSupport;

import static org.assertj.core.api.Assertions.assertThat;

public class BridgeExecutorTest {

    @Test
    public void fromDTO() {
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();
        BridgeExecutor bridgeExecutor = BridgeExecutor.fromDTO(dto, "ns", "image");

        assertThat(bridgeExecutor.getMetadata().getNamespace()).isEqualTo("ns");
        assertThat(bridgeExecutor.getMetadata().getName()).isEqualTo(BridgeExecutor.OB_RESOURCE_NAME_PREFIX + TestSupport.PROCESSOR_ID);
        assertThat(bridgeExecutor.getMetadata().getLabels().get(LabelsBuilder.CREATED_BY_LABEL)).isEqualTo(LabelsBuilder.OPERATOR_NAME);
        assertThat(bridgeExecutor.getMetadata().getLabels().get(LabelsBuilder.MANAGED_BY_LABEL)).isEqualTo(LabelsBuilder.OPERATOR_NAME);
        assertThat(bridgeExecutor.getMetadata().getLabels().get(LabelsBuilder.COMPONENT_LABEL)).isEqualTo(BridgeExecutor.COMPONENT_NAME);

        assertThat(bridgeExecutor.getSpec().getProcessorName()).isEqualTo(dto.getName());
        assertThat(bridgeExecutor.getSpec().getId()).isEqualTo(dto.getId());
        assertThat(bridgeExecutor.getSpec().getImage()).isEqualTo("image");
        assertThat(bridgeExecutor.getSpec().getBridgeId()).isEqualTo(TestSupport.BRIDGE_ID);
        assertThat(bridgeExecutor.getSpec().getOwner()).isEqualTo(dto.getOwner());
    }
}
