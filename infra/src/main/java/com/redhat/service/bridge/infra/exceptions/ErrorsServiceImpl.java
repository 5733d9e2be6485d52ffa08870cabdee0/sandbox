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
    public ListResult<Error> getErrors(QueryInfo queryInfo) {
        return repository.findAll(queryInfo);
    }

    @Override
    public Optional<Error> getError(int errorId) {
        return Optional.ofNullable(repository.findById(errorId));
    }

    @Override
    public Optional<Error> getError(Exception e) {
        return Optional.ofNullable(repository.findByException(e));

    }
}
