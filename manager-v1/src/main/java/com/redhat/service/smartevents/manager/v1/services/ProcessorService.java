package com.redhat.service.smartevents.manager.v1.services;

import java.util.List;
import java.util.Optional;

import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.v1.api.dto.ProcessorManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.v1.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v1.api.models.queries.QueryProcessorResourceInfo;
import com.redhat.service.smartevents.manager.v1.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.v1.api.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.v1.persistence.models.Processor;

public interface ProcessorService {

    Processor getProcessor(String bridgeId, String processorId, String customerId);

    Processor createProcessor(String bridgeId, String customerId, String owner, String organisationId, ProcessorRequest processorRequest);

    Processor updateProcessor(String bridgeId, String processorId, String customerId, ProcessorRequest processorRequest);

    Optional<Processor> getErrorHandler(String bridgeId, String customerId);

    Processor createErrorHandlerProcessor(String bridgeId, String customerId, String owner, ProcessorRequest processorRequest);

    Processor updateErrorHandlerProcessor(String bridgeId, String processorId, String customerId, ProcessorRequest processorRequest);

    List<Processor> findByShardIdToDeployOrDelete(String shardId);

    Processor updateProcessorStatus(ProcessorManagedResourceStatusUpdateDTO updateDTO);

    Long getProcessorsCount(String bridgeId, String customerId);

    ListResult<Processor> getProcessors(String bridgeId, String customerId, QueryProcessorResourceInfo queryInfo);

    void deleteProcessor(String bridgeId, String processorId, String customerId);

    ProcessorDTO toDTO(Processor processor);

    ProcessorResponse toResponse(Processor processor);
}
