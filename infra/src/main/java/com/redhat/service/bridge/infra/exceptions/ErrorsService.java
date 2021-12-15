package com.redhat.service.bridge.infra.exceptions;

import java.util.Optional;

import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;

public interface ErrorsService {

    ListResult<Error> getErrors(QueryInfo pageInfo);

    Optional<Error> getError(int errorId);

    Optional<Error> getError(Exception e);
}
