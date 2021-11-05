package com.redhat.service.bridge.manager;

import java.util.Optional;

import com.redhat.service.bridge.manager.models.Error;
import com.redhat.service.bridge.manager.models.ListResult;
import com.redhat.service.bridge.manager.models.QueryInfo;

public interface ErrorsService {

    ListResult<Error> getErrors(QueryInfo pageInfo);

    Optional<Error> getError(int errorId);

    Optional<Error> getError(Exception e);
}
