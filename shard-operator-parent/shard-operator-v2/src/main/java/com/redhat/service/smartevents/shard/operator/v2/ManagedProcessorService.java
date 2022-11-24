package com.redhat.service.smartevents.shard.operator.v2;

import java.util.List;

import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;
import com.redhat.service.smartevents.shard.operator.v2.resources.CamelIntegration;

public interface ManagedProcessorService {

    void createManagedProcessor(ProcessorDTO bridgeDTO, String namespace);

    CamelIntegration fetchOrCreateCamelIntegration(ManagedProcessor processor, String integrationName);

    void deleteManagedProcessor(ProcessorDTO processorDTO);

    List<ManagedProcessor> fetchAllManagedProcessors();
}
