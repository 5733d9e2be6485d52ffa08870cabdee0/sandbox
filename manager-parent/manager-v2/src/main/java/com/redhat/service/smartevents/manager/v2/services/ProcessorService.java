package com.redhat.service.smartevents.manager.v2.services;

import java.util.List;

import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ResourceStatusDTO;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;

public interface ProcessorService {

    Processor getProcessor(String bridgeId, String processorId, String customerId);

    ListResult<Processor> getProcessors(String bridgeId, String customerId, QueryResourceInfo queryInfo);

    Processor createProcessor(String bridgeId, String customerId, String owner, String organisationId, ProcessorRequest processorRequest);

    Long getProcessorsCount(String bridgeId, String customerId);

    Processor updateProcessor(String bridgeId, String processorId, String customerId, ProcessorRequest processorRequest);

    void deleteProcessor(String bridgeId, String processorId, String customerId);

    List<Processor> findByShardIdToDeployOrDelete(String shardId);

    Processor updateProcessorStatus(ResourceStatusDTO statusDTO);

    ProcessorDTO toDTO(Processor processor);

    ProcessorResponse toResponse(Processor processor);
}
