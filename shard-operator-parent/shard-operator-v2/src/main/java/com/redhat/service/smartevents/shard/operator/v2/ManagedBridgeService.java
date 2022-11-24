package com.redhat.service.smartevents.shard.operator.v2;

import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;

public interface ManagedBridgeService {

    void createManagedBridgeResources(ManagedBridge managedBridge);

    void deleteManagedBridgeResources(ManagedBridge managedBridge);
}
