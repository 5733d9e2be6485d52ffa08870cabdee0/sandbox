package com.redhat.service.smartevents.manager.v1.services;

import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.manager.core.api.models.responses.ProcessingErrorResponse;
import com.redhat.service.smartevents.manager.core.persistence.models.ProcessingError;

public interface ProcessingErrorService {

    String ENDPOINT_ERROR_HANDLER_TYPE = "endpoint";

    static boolean isEndpointErrorHandlerAction(Action action) {
        return action != null && ENDPOINT_ERROR_HANDLER_TYPE.equals(action.getType());
    }

    ListResult<ProcessingError> getProcessingErrors(String bridgeId, String customerId, QueryResourceInfo queryInfo);

    Action resolveAndUpdateErrorHandler(String bridgeId, Action errorHandler);

    ProcessingErrorResponse toResponse(ProcessingError processingError);
}
