package com.redhat.service.bridge.manager.workers;

import java.util.Set;

import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.models.ManagedResource;
import com.redhat.service.bridge.manager.models.Work;

/**
 * Handles completion of {@link Work} for a {@link ManagedResource}.
 *
 * @param <T>
 */
public interface Worker<T extends ManagedResource> {

    Set<ManagedResourceStatus> PROVISIONING_STARTED = Set.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.PROVISIONING);
    Set<ManagedResourceStatus> DEPROVISIONING_STARTED = Set.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETING);

    Set<ManagedResourceStatus> PROVISIONING_COMPLETED = Set.of(ManagedResourceStatus.READY, ManagedResourceStatus.FAILED);
    Set<ManagedResourceStatus> DEPROVISIONING_COMPLETED = Set.of(ManagedResourceStatus.DELETED, ManagedResourceStatus.FAILED);

    /**
     * Execute work. When complete {@link WorkManager#complete(Work)} should be invoked.
     * 
     * @param work
     * @return true if work is considered complete.
     */
    boolean handleWork(Work work);

}