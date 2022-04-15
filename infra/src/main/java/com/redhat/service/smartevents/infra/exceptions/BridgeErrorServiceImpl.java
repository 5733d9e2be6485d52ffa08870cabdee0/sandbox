package com.redhat.service.smartevents.infra.exceptions;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryInfo;

@ApplicationScoped
public class BridgeErrorServiceImpl implements BridgeErrorService {

    @Inject
    BridgeErrorDAO repository;

    @Override
    public ListResult<BridgeError> getUserErrors(QueryInfo queryInfo) {
        return repository.findAllErrorsByType(queryInfo, BridgeErrorType.USER);
    }

    @Override
    public Optional<BridgeError> getUserError(int errorId) {
        return Optional.ofNullable(repository.findErrorByIdAndType(errorId, BridgeErrorType.USER));
    }

    @Override
    public Optional<BridgeError> getError(Exception e) {
        return Optional.ofNullable(repository.findByException(e));
    }

    @Override
    public Optional<BridgeError> getError(Class clazz) {
        return Optional.ofNullable(repository.findByException(clazz));
    }
}
