package com.redhat.service.smartevents.manager.v2.workers.resources;

import com.redhat.service.smartevents.manager.core.models.ManagedResource;
import com.redhat.service.smartevents.manager.core.workers.Work;
import com.redhat.service.smartevents.manager.core.workers.Worker;

public interface WorkerV2<T extends ManagedResource> extends Worker<T> {

    /**
     * Checks if the worker accepts a {@link Work}.
     *
     * @param work The {@link Work} to execute.
     * @return <code>true</code> if the worker accepts the {@link Work}, <code>false</code> otherwise.
     */
    boolean accept(Work work);
}
