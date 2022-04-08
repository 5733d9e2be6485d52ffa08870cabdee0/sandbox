package com.redhat.service.smartevents.shard.operator;

import io.smallrye.mutiny.Uni;

public interface ManagerSyncService {
    Uni<Object> fetchAndProcessBridgesToDeployOrDelete();

    Uni<Object> fetchAndProcessProcessorsToDeployOrDelete();
}
