package com.redhat.service.smartevents.shard.operator.v2;

public interface ManagedBridgeSyncService {

    void syncManagedBridgeWithManager();

    void syncManagedBridgeStatusBackToManager();
}
