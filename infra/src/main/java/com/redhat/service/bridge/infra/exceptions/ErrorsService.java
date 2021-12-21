package com.redhat.service.bridge.infra.exceptions;

import java.util.Optional;

import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;

public interface ErrorsService {

    ListResult<Error> getUserErrors(QueryInfo pageInfo);

    Optional<Error> getUserError(int errorId);

    Optional<Error> getError(Exception e);

    Optional<Error> getError(Class clazz);
}
