package com.redhat.service.smartevents.shard.operator.v1.resources;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionTypeConstants;
import com.redhat.service.smartevents.shard.operator.v1.TestSupport;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionTest {

    @Test
    void testKubernetesDateFormat() {
        final ObjectMapper mapper = new ObjectMapper();
        final BridgeIngress ingress = BridgeIngress.fromDTO(TestSupport.newRequestedBridgeDTO(), "ns");
        ingress.getStatus().markConditionTrue(ConditionTypeConstants.READY);
        final JsonNode conditionAsNode = mapper.convertValue(ingress.getStatus().getConditionByType(ConditionTypeConstants.READY).get(), JsonNode.class);
        assertThat(conditionAsNode.get("lastTransitionTime").getNodeType()).isEqualTo(JsonNodeType.STRING);
        assertThat(conditionAsNode.get("lastTransitionTime").asText()).contains("T");
        assertThat(conditionAsNode.get("lastTransitionTime").asText()).contains("Z");
    }
}
