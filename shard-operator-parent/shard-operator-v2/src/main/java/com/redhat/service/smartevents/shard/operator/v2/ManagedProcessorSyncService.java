package com.redhat.service.smartevents.shard.operator.v2;

public interface ManagedProcessorSyncService {

    void syncManagedProcessorWithManager();

    void syncManagedProcessorStatusBackToManager();
}
