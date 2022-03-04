package com.redhat.service.bridge.manager.workers.id;

import java.util.UUID;

import javax.enterprise.inject.Produces;

public class WorkerIdProvider {

    private static final String WORKER_ID = UUID.randomUUID().toString();

    @Produces
    public String getWorkerId() {
        return WORKER_ID;
    }

}