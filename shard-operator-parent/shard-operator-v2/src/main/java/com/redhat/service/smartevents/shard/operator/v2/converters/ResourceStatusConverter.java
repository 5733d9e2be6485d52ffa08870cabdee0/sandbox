package com.redhat.service.smartevents.shard.operator.v2.converters;

import java.util.List;

import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ResourceStatusDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;

public class ResourceStatusConverter {

    public static ResourceStatusDTO fromManagedProcessorToResourceStatusDTO(ManagedProcessor managedProcessor) {
        List<ConditionDTO> conditionDTOs = ConditionConverter.fromConditionsToConditionDTOs(managedProcessor.getStatus().getConditions());
        return new ResourceStatusDTO(
                managedProcessor.getSpec().getId(),
                managedProcessor.getSpec().getGeneration(),
                conditionDTOs);
    }

    public static ResourceStatusDTO fromManagedBridgeToResourceStatusDTO(ManagedBridge managedBridge) {
        List<ConditionDTO> conditionDTOs = ConditionConverter.fromConditionsToConditionDTOs(managedBridge.getStatus().getConditions());
        return new ResourceStatusDTO(
                managedBridge.getSpec().getId(),
                managedBridge.getSpec().getGeneration(),
                conditionDTOs);
    }
}
