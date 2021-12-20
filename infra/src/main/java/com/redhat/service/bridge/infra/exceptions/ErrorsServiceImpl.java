package com.redhat.service.bridge.infra.exceptions;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;

@ApplicationScoped
public class ErrorsServiceImpl implements ErrorsService {

    @Inject
    ErrorDAO repository;

    @Override
    public ListResult<Error> getUserErrors(QueryInfo queryInfo) {
        return repository.findAllUserErrors(queryInfo);
    }

    @Override
    public Optional<Error> getUserError(int errorId) {
        return Optional.ofNullable(repository.findUserErrorById(errorId));
    }

    @Override
    public Optional<Error> getError(Exception e) {
        return Optional.ofNullable(repository.findByException(e));

    }
}
