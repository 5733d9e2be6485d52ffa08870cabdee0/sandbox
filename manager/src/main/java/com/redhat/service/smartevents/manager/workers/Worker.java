package com.redhat.service.smartevents.manager.workers;

import com.redhat.service.smartevents.manager.models.ManagedResource;
import com.redhat.service.smartevents.manager.models.Work;

/**
 * Handles completion of {@link Work} for a {@link ManagedResource}.
 *
 * @param <T>
 */
public interface Worker<T extends ManagedResource> {

    /**
     * Execute work. When complete {@link WorkManager#complete(Work)} should be invoked.
     * 
     * @param work
     * @return The updated resource.
     */
    T handleWork(Work work);

}