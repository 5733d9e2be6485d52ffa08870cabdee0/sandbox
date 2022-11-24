package com.redhat.service.smartevents.shard.operator.v2;

import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.CamelIntegration;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;

public interface ManagedProcessorService {

    void createManagedProcessor(ProcessorDTO bridgeDTO, String namespace);

    CamelIntegration fetchOrCreateCamelIntegration(ManagedProcessor processor, String integrationName);

    void deleteManagedProcessor(ProcessorDTO processorDTO);
}
