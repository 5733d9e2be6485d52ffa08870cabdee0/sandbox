package com.redhat.service.smartevents.manager.core.workers;

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

    /**
     * Checks if the worker accepts a {@link Work}.
     *
     * @param work The {@link Work} to execute.
     * @return <code>true</code> if the worker accepts the {@link Work}, <code>false</code> otherwise.
     */
    boolean accept(Work work);
}