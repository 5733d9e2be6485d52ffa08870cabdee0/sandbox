package com.redhat.service.smartevents.manager.v2.services;

import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.queries.QueryResourceInfo;
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

    ProcessorResponse toResponse(Processor processor);
}
