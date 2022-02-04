package com.redhat.service.bridge.shard.operator.resources;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.shard.operator.TestSupport;

import static org.assertj.core.api.Assertions.assertThat;

public class BridgeExecutorTest {

    @Test
    public void fromDTO() {
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();
        BridgeExecutor bridgeExecutor = BridgeExecutor.fromDTO(dto, "ns", "image");

        assertThat(bridgeExecutor.getMetadata().getNamespace()).isEqualTo("ns");
        assertThat(bridgeExecutor.getMetadata().getName()).isEqualTo("ob-" + TestSupport.PROCESSOR_ID);

        assertThat(bridgeExecutor.getSpec().getProcessorName()).isEqualTo(dto.getName());
        assertThat(bridgeExecutor.getSpec().getId()).isEqualTo(dto.getId());
        assertThat(bridgeExecutor.getSpec().getImage()).isEqualTo("image");
        assertThat(bridgeExecutor.getSpec().getBridgeId()).isEqualTo(TestSupport.BRIDGE_ID);
    }
}
