package com.redhat.service.bridge.executor;

import java.util.Set;

import javax.annotation.PostConstruct;

// TODO: Annotate this class with @ApplicationScoped when we move away from ExecutorConfigProviderMock
public class ExecutorConfigProviderImpl implements ExecutorConfigProvider {

    private Set<Executor> executors;

    @PostConstruct
    void init(){
        // TODO: read configuration

        // TODO: create and set executors
    }

    @Override
    public Set<Executor> getExecutors() {
        return executors;
    }

    @Override
    public Set<Executor> getExecutors(String bridgeId) {
        throw new UnsupportedOperationException("Not implemented.");
    }
}
