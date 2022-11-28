package com.redhat.service.smartevents.manager.v1.workers;

import com.redhat.service.smartevents.manager.core.models.ManagedResource;

/**
 * Handles completion of {@link Work} for a {@link ManagedResource}.
 *
 * @param <T>
 */
public interface Worker<T extends ManagedResource> {

    /**
     * Execute work.
     *
     * @param work The {@link Work} to execute.
     * @return The updated resource.
     */
    T handleWork(Work work);

}