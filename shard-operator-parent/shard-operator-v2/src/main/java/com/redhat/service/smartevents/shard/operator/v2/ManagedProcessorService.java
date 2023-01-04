package com.redhat.service.smartevents.shard.operator.v2;

import java.util.List;

import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;

public interface ManagedProcessorService {

    void createManagedProcessor(ProcessorDTO processorDTO);

    void deleteManagedProcessor(ProcessorDTO processorDTO);

    List<ManagedProcessor> fetchAllManagedProcessors();
}
