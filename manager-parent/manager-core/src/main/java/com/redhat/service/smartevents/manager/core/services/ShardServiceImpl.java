package com.redhat.service.smartevents.manager.core.services;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.manager.core.persistence.dao.ShardDAO;
import com.redhat.service.smartevents.manager.core.persistence.models.Shard;

@ApplicationScoped
public class ShardServiceImpl implements ShardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShardServiceImpl.class);

    private Set<String> shards;

    @Inject
    ShardDAO shardDAO;

    @PostConstruct
    public void init() {
        /*
         * Fetch the list of authorized shards at startup.
         * This needs to be changed when admin api will include the CRUD of a shard at runtime.
         */
        shards = shardDAO.listAll().stream().map(Shard::getId).collect(Collectors.toSet());
    }

    @Override
    public Shard getAssignedShard(String id) {
        List<Shard> shards = shardDAO.listAll();

        // add the assignment logic here
        if (shards.size() != 1) {
            LOGGER.warn("There are more than 1 available shards but no assignment strategy has been implemented. This situation is not supported yet. Using the first in the list.");
        }

        return shards.get(0);
    }

    @Override
    public boolean isAuthorizedShard(String shardId) {
        return shards.contains(shardId);
    }
}
