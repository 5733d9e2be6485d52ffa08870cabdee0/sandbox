package com.redhat.service.smartevents.shard.operator.v2.converters;

import java.util.List;

import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorStatusDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;

public class ProcessorStatusConverter {

    public static ProcessorStatusDTO fromManagedProcessorToProcessorStatusDTO(ManagedProcessor managedProcessor) {
        List<ConditionDTO> conditionDTOs = ConditionConverter.fromConditionsToConditionDTOs(managedProcessor.getStatus().getConditions());
        return new ProcessorStatusDTO(
                managedProcessor.getSpec().getId(),
                managedProcessor.getSpec().getGeneration(),
                conditionDTOs);
    }
}
