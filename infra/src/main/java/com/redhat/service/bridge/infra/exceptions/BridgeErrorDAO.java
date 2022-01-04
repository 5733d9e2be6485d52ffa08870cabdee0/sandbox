package com.redhat.service.bridge.infra.exceptions;

import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;

public interface BridgeErrorDAO {

    ListResult<BridgeError> findAllErrorsByType(QueryInfo queryInfo, BridgeErrorType type);

    BridgeError findErrorByIdAndType(int errorId, BridgeErrorType type);

    BridgeError findByException(Exception ex);

    BridgeError findByException(Class clazz);
}
