package com.redhat.service.bridge.manager.providers;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WorkerIdProviderImpl implements WorkerIdProvider {

    private static final String WORKER_ID = UUID.randomUUID().toString();

    @Override
    public String getWorkerId() {
        return WORKER_ID;
    }
}
