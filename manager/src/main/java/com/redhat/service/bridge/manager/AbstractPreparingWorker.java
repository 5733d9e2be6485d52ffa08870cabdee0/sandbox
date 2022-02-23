package com.redhat.service.bridge.manager;

import com.redhat.service.bridge.manager.models.ManagedEntity;

public interface AbstractPreparingWorker<T extends ManagedEntity> {

    void accept(T entity);

    void reschedule();

    void discoverOrphanWorkers();
}
