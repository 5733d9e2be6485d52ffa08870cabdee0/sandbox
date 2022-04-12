package com.redhat.service.smartevents.infra.exceptions;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryInfo;

public interface BridgeErrorDAO {

    ListResult<BridgeError> findAllErrorsByType(QueryInfo queryInfo, BridgeErrorType type);

    BridgeError findErrorByIdAndType(int errorId, BridgeErrorType type);

    BridgeError findByException(Exception ex);

    BridgeError findByException(Class clazz);
}
