package com.redhat.service.smartevents.shard.operator.v2.converters;

import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;

public class ManagedProcessorConverter {

    public static ManagedProcessor fromProcessorDTOToManagedProcessor(ProcessorDTO processorDTO, String namespace) {
        return new ManagedProcessor.Builder()
                .withNamespace(namespace)
                .withProcessorId(processorDTO.getId())
                .withBridgeId(processorDTO.getBridgeId())
                .withCustomerId(processorDTO.getCustomerId())
                .withDefinition(processorDTO.getFlows())
                .withProcessorName(processorDTO.getName())
                .withGeneration(processorDTO.getGeneration())
                .build();
    }
}
