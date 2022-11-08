package com.redhat.service.smartevents.infra.core.exceptions;

import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.queries.QueryPageInfo;

public interface BridgeErrorDAO {

    ListResult<BridgeError> findAllErrorsByType(QueryPageInfo queryInfo, BridgeErrorType type);

    BridgeError findErrorById(int errorId);

    BridgeError findErrorByIdAndType(int errorId, BridgeErrorType type);

    BridgeError findByException(Exception ex);

    BridgeError findByException(Class clazz);
}
