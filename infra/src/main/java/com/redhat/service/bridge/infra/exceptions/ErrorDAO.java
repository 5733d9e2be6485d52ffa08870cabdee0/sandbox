package com.redhat.service.bridge.infra.exceptions;

import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;

public interface ErrorDAO {

    ListResult<Error> findAllUserErrors(QueryInfo queryInfo);

    Error findUserErrorById(int errorId);

    Error findByException(Exception ex);
}
