package com.redhat.service.smartevents.manager.core.workers;

import com.redhat.service.smartevents.manager.core.models.ManagedResource;

public interface WorkManager {

    /**
     * Request {@link Work} be scheduled for the given {@link ManagedResource}.
     *
     * @param managedResource
     * @return The {@link Work}.
     */
    Work schedule(ManagedResource managedResource);

    /**
     * Request the {@link Work} is re-scheduled.
     *
     * @param work
     */
    void reschedule(Work work);

    /**
     * Request the {@link Work} is re-scheduled and a failed attempt is recorded.
     *
     * @param work
     */
    void rescheduleAfterFailure(Work work);

    /**
     * Checks if {@link Work} remains to be completed for the {@link ManagedResource}.
     *
     * @param managedResource
     * @return true if {@link Work} remains incomplete for the {@link ManagedResource}.
     */
    boolean exists(ManagedResource managedResource);
}