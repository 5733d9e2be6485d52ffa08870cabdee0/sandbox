package com.redhat.service.smartevents.manager.v2.services;

import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;

public interface ProcessorService {

    Processor createProcessor(String bridgeId, String customerId, String owner, String organisationId, ProcessorRequest processorRequest);

    Long getProcessorsCount(String bridgeId, String customerId);

    ProcessorResponse toResponse(Processor processor);
}
