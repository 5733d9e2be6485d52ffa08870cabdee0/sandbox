package com.redhat.service.bridge.manager.workers;

import com.redhat.service.bridge.manager.models.ManagedEntity;

/*
    A WorkManager provides a way to request that work is executed on a ManagedEntity. How the work
    is scheduled and then processed is completely up to the WorkManager. E.g it could be in-memory, use a DB
    to self manage a work queue, or perhaps delegate out to a more appropriate technology selection e.g Kafka.
 */
public interface WorkManager {

    /*
        Request that work in scheduled to prepare the resources on which this entity depends
     */
    Work scheduleWork(ManagedEntity managedEntity);

    /*
        Let the work manager know the work has been completed and can be discarded from the work queue
     */
    void completeWork(Work work);

}
