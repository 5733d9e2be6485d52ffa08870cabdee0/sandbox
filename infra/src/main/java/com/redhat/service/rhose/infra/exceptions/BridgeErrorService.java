package com.redhat.service.rhose.infra.exceptions;

import java.util.Optional;

import com.redhat.service.rhose.infra.models.ListResult;
import com.redhat.service.rhose.infra.models.QueryInfo;

public interface BridgeErrorService {

    ListResult<BridgeError> getUserErrors(QueryInfo pageInfo);

    Optional<BridgeError> getUserError(int errorId);

    Optional<BridgeError> getError(Exception e);

    Optional<BridgeError> getError(Class clazz);
}
