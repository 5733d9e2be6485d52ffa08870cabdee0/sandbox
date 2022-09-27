package com.redhat.service.smartevents.infra.exceptions;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.queries.QueryPageInfo;

@ApplicationScoped
public class BridgeErrorServiceImpl implements BridgeErrorService {

    @Inject
    BridgeErrorDAO repository;

    @Override
    public ListResult<BridgeError> getUserErrors(QueryPageInfo queryInfo) {
        return repository.findAllErrorsByType(queryInfo, BridgeErrorType.USER);
    }

    @Override
    public Optional<BridgeError> getUserError(int errorId) {
        return Optional.ofNullable(repository.findErrorByIdAndType(errorId, BridgeErrorType.USER));
    }

    @Override
    public Optional<BridgeError> getPlatformError(int errorId) {
        return Optional.ofNullable(repository.findErrorByIdAndType(errorId, BridgeErrorType.PLATFORM));
    }

    @Override
    public Optional<BridgeError> getError(Exception e) {
        return Optional.ofNullable(repository.findByException(e));
    }

    @Override
    public Optional<BridgeError> getError(Class clazz) {
        return Optional.ofNullable(repository.findByException(clazz));
    }

    @Override
    public Optional<BridgeError> getError(int errorId) {
        return Optional.ofNullable(repository.findErrorById(errorId));
    }
}
