package com.redhat.service.smartevents.infra.core.exceptions;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@ApplicationScoped
public class CompositeBridgeErrorServiceImpl implements CompositeBridgeErrorService {

    private List<BridgeErrorDAO> repositories;

    protected CompositeBridgeErrorServiceImpl() {
        // CDI proxy
    }

    @Inject
    public CompositeBridgeErrorServiceImpl(@Any Instance<BridgeErrorDAO> instances) {
        this.repositories = instances.stream().collect(Collectors.toList());
    }

    @Override
    public Optional<BridgeError> getError(Class clazz) {
        return repositories.stream().map(dao -> dao.findByException(clazz)).filter(Objects::nonNull).findFirst();
    }
}
