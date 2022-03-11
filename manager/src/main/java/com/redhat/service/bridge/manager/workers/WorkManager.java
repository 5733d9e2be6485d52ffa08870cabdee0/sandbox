package com.redhat.service.bridge.manager.workers;

import com.redhat.service.bridge.manager.models.ManagedResource;
import com.redhat.service.bridge.manager.models.Work;

/**
 * Handles the completion of {@link Work} for {@link ManagedResource}.
 * If a suitable {@link Worker} is not defined for a given {@link ManagedResource}
 * the {@link Work} will remain incomplete indefinitely.
 */
public interface WorkManager {

    /**
     * Request {@link Work} be scheduled for the given {@link ManagedResource}.
     * 
     * @param managedResource
     * @return
     */
    Work schedule(ManagedResource managedResource);

    /**
     * Checks if {@link Work} remains to be completed. The Work reference returned from
     * {@link WorkManager#schedule(ManagedResource)} can become stale if held onto
     * longer than the Work takes to complete.
     * 
     * @param work
     * @return true if {@link Work} remains incomplete.
     */
    boolean exists(Work work);

    /**
     * Records an attempt to complete an item of {@link Work}
     * 
     * @param work
     */
    void recordAttempt(Work work);

    /**
     * Marks {@link Work} as complete so that it can be removed from the work queue.
     * 
     * @param work
     */
    void complete(Work work);

}