package com.redhat.service.smartevents.infra.exceptions;

import java.util.Optional;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryPageInfo;

public interface BridgeErrorService {

    ListResult<BridgeError> getUserErrors(QueryPageInfo pageInfo);

    Optional<BridgeError> getUserError(int errorId);

    Optional<BridgeError> getError(Exception e);

    Optional<BridgeError> getError(Class clazz);
}
