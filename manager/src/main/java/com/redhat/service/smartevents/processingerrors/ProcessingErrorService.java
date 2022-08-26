package com.redhat.service.smartevents.processingerrors;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processingerrors.api.models.ProcessingErrorResponse;
import com.redhat.service.smartevents.processingerrors.models.ProcessingError;

public interface ProcessingErrorService {

    String ENDPOINT_ERROR_HANDLER_TYPE = "endpoint";

    static boolean isEndpointErrorHandlerAction(Action action) {
        return action != null && ENDPOINT_ERROR_HANDLER_TYPE.equals(action.getType());
    }

    ListResult<ProcessingError> getProcessingErrors(String bridgeId, String customerId, QueryResourceInfo queryInfo);

    Action resolveAndUpdateErrorHandler(String bridgeId, Action errorHandler);

    ProcessingErrorResponse toResponse(ProcessingError processingError);
}
