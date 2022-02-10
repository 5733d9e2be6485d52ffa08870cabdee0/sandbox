package com.redhat.service.bridge.manager;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.manager.dao.ShardDAO;
import com.redhat.service.bridge.manager.models.Shard;
import com.redhat.service.bridge.manager.models.ShardType;

@ApplicationScoped
public class ShardAssignServiceImpl implements ShardAssignService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShardAssignServiceImpl.class);

    @Inject
    ShardDAO shardDAO;

    @Override
    public String getAssignedShardId(String id) {
        List<Shard> shards = shardDAO.findByType(ShardType.TRADITIONAL);

        // add the assignment logic here
        if (shards.size() != 1) {
            LOGGER.error("The number of 'TRADITIONAL' shards is not equal to 1. This situation is not supported yet. Using the first in the list.");
        }

        return shards.get(0).getId();
    }
}
