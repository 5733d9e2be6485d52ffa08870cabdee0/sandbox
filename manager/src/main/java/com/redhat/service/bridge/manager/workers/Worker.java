package com.redhat.service.bridge.manager.workers;

import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.models.ManagedResource;
import com.redhat.service.bridge.manager.models.Work;

/**
 * Handles completion of {@link Work} for a {@link ManagedResource}.
 * 
 * If {@link ManagedResource#getStatus()} is {@link ManagedResourceStatus#ACCEPTED} it is assumed the {@link ManagedResource} needs to
 * be created and its dependencies created by {@link Worker#createDependencies(Work, ManagedResource)}.
 * 
 * If the {@link ManagedResource#getStatus()} is {@link ManagedResourceStatus#DEPROVISION} it is assumed the {@link ManagedResource} needs to
 * be destroyed and its dependencies deleted by {@link Worker#deleteDependencies(Work, ManagedResource)}.
 * 
 * Other values of {@link ManagedResourceStatus} when the {@link Work} is {@link WorkManager#schedule(ManagedResource)} will result
 * in the {@link Work} remaining incomplete indefinitely.
 * 
 * @param <T>
 */
public interface Worker<T extends ManagedResource> {

    /**
     * Execute work. When complete {@link WorkManager#complete(Work)} should be invoked.
     * 
     * @param work
     * @return true if the Work was processed, otherwise false.
     */
    /*
     * A return value is not really needed however there is an issue with (possibly) Quarkus and
     * {@link EventBus#requestAndForget(String, Object)}. If a value is not returned the logs shows
     * occurrences of "ERROR: Failed to send reply.(TIMEOUT,-1) Timed out after waiting 30000(ms)".
     * See https://stackoverflow.com/questions/49449257/vertx-timeout-in-message-reply
     */
    boolean handleWork(Work work);

    /**
     * Creates dependent resources required by the {@link ManagedResource}.
     *
     * @param managedResource
     * @return
     */
    T createDependencies(T managedResource);

    /**
     * Deletes dependent resources that were required by the {@link ManagedResource}.
     *
     * @param managedResource
     * @return
     */
    T deleteDependencies(T managedResource);
}