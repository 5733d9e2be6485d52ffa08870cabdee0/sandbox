package com.redhat.service.bridge.manager.workers.id;

import java.util.UUID;

import javax.enterprise.inject.Produces;

public class WorkerIdProviderImpl implements WorkerIdProvider {

    private static final String ID = UUID.randomUUID().toString();

    private static final WorkerId WORKER_ID = () -> ID;

    @Override
    @Produces
    public WorkerId getWorkerId() {
        return WORKER_ID;
    }

}