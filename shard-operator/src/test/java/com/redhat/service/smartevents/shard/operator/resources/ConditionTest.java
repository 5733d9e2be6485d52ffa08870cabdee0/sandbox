package com.redhat.service.smartevents.shard.operator.resources;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.redhat.service.smartevents.shard.operator.TestSupport;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionTest {

    @Test
    void testKubernetesDateFormat() {
        final ObjectMapper mapper = new ObjectMapper();
        final BridgeIngress ingress = BridgeIngress.fromDTO(TestSupport.newRequestedBridgeDTO(), "ns");
        ingress.getStatus().markConditionTrue(ConditionType.Ready);
        final JsonNode conditionAsNode = mapper.convertValue(ingress.getStatus().getConditionByType(ConditionType.Ready).get(), JsonNode.class);
        assertThat(conditionAsNode.get("lastTransitionTime").getNodeType()).isEqualTo(JsonNodeType.STRING);
        assertThat(conditionAsNode.get("lastTransitionTime").asText()).contains("T");
        assertThat(conditionAsNode.get("lastTransitionTime").asText()).contains("Z");
    }
}
