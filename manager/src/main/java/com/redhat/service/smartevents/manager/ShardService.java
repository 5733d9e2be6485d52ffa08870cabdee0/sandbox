package com.redhat.service.smartevents.manager;

import com.redhat.service.smartevents.manager.models.Shard;

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
