package com.redhat.service.smartevents.manager.core.services;

import com.redhat.service.smartevents.manager.core.persistence.models.Shard;

public interface ShardService {

    Shard getAssignedShard(String id);

    boolean isAuthorizedShard(String shardId);

    /**
     * TODO: Shard-admin api to be implemented.
     * void addShard();
     * void deleteShard();
     * ...
     */
}
