package com.redhat.service.smartevents.manager.services.v1;

import com.redhat.service.smartevents.manager.persistence.v1.models.Shard;

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
