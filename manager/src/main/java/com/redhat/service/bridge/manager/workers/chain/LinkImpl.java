package com.redhat.service.bridge.manager.workers.chain;

import java.util.Objects;

import com.redhat.service.bridge.manager.models.ManagedResource;
import com.redhat.service.bridge.manager.workers.Worker;

public class LinkImpl<T extends ManagedResource> implements Link<T> {

    private final Worker<T> worker;
    private final T managedResource;

    public LinkImpl(Worker<T> worker,
            T managedResource) {
        this.worker = Objects.requireNonNull(worker);
        this.managedResource = Objects.requireNonNull(managedResource);
    }

    @Override
    public T getManagedResource() {
        return managedResource;
    }

    @Override
    public Worker<T> getSourceWorker() {
        return worker;
    }

}
