package com.redhat.service.smartevents.shard.operator.v2.converters;

import java.util.List;

import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeStatusDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;

public class BridgeStatusConverter {

    public static BridgeStatusDTO fromManagedBridgeToBridgeStatusDTO(ManagedBridge managedBridge) {
        List<ConditionDTO> conditionDTOs = ConditionConverter.fromConditionsToConditionDTOs(managedBridge.getStatus().getConditions());
        return new BridgeStatusDTO(
                managedBridge.getSpec().getId(),
                managedBridge.getSpec().getGeneration(),
                conditionDTOs);
    }
}
