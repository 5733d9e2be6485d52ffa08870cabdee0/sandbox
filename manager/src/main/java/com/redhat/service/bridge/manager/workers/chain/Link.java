package com.redhat.service.bridge.manager.workers.chain;

import com.redhat.service.bridge.manager.models.ManagedResource;
import com.redhat.service.bridge.manager.workers.Worker;

public interface Link<S extends ManagedResource> {

    Worker<S> getSourceWorker();

    S getManagedResource();

}
