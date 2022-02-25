package com.redhat.service.bridge.manager.workers;

import com.redhat.service.bridge.manager.models.ManagedEntity;

/*
    A worker handles work to create or delete dependencies of the ManagedEntity on which it operates

    Workers should be idempotent and observe the environment to determine if the resources that they
    are responsible to provision are ready.

 */
public interface Worker<T extends ManagedEntity> {

    T createDependencies(T managedEntity);

    T deleteDependencies(T managedEntity);
}
