package com.redhat.service.smartevents.processingerrors;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processingerrors.api.models.ProcessingErrorResponse;
import com.redhat.service.smartevents.processingerrors.models.ProcessingError;

public interface ProcessingErrorService {

    Action getEndpointErrorHandlerResolvedAction();

    String getErrorEndpoint(String bridgeId);

    ListResult<ProcessingError> getProcessingErrors(String bridgeId, String customerId, QueryResourceInfo queryInfo);

    ProcessingErrorResponse toResponse(ProcessingError processingError);
}
