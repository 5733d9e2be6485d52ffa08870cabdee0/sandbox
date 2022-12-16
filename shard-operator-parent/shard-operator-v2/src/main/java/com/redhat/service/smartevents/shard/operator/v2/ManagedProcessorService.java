package com.redhat.service.smartevents.shard.operator.v2;

import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;

public interface ManagedProcessorService {

    void createManagedProcessor(ProcessorDTO processorDTO);

    void deleteManagedProcessor(ProcessorDTO processorDTO);
}
