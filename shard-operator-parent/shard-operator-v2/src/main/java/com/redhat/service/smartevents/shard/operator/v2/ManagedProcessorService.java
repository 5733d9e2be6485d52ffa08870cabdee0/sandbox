package com.redhat.service.smartevents.shard.operator.v2;

import java.util.List;

import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;
import com.redhat.service.smartevents.shard.operator.v2.resources.camel.CamelIntegration;

public interface ManagedProcessorService {

    void createManagedProcessor(ProcessorDTO bridgeDTO);

    CamelIntegration fetchOrCreateCamelIntegration(ManagedProcessor processor);

    void deleteManagedProcessor(ProcessorDTO processorDTO);

    List<ManagedProcessor> fetchAllManagedProcessors();
}
