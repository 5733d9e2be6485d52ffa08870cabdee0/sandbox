package com.redhat.service.rhose.infra.exceptions;

import com.redhat.service.rhose.infra.models.ListResult;
import com.redhat.service.rhose.infra.models.QueryInfo;

public interface BridgeErrorDAO {

    ListResult<BridgeError> findAllErrorsByType(QueryInfo queryInfo, BridgeErrorType type);

    BridgeError findErrorByIdAndType(int errorId, BridgeErrorType type);

    BridgeError findByException(Exception ex);

    BridgeError findByException(Class clazz);
}
