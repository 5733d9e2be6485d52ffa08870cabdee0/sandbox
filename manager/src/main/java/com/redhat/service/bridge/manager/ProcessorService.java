package com.redhat.service.bridge.manager;

import java.util.List;

import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.Processor;

public interface ProcessorService {

    Processor getProcessor(String processorId, Bridge bridge);

    Processor createProcessor(Bridge bridge, ProcessorRequest processorRequest);

    List<Processor> getProcessorByStatusesAndShardIdWithReadyDependencies(List<BridgeStatus> statuses, String shardId);

    Processor updateProcessorStatus(ProcessorDTO processorDTO);

    Long getProcessorsCount(Bridge bridge);

    ListResult<Processor> getProcessors(Bridge bridge, QueryInfo queryInfo);

    void deleteProcessor(Bridge bridge, String processorId);

    ProcessorDTO toDTO(Processor processor);

    ProcessorResponse toResponse(Processor processor);
}
