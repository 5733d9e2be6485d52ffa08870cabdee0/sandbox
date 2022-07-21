package com.redhat.service.smartevents.manager;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.dao.ShardDAO;
import com.redhat.service.smartevents.manager.models.Shard;
import com.redhat.service.smartevents.manager.models.ShardType;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ShardServiceTest {

    @Inject
    ShardDAO shardDAO;

    @Inject
    ShardService shardService;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @Test
    public void testGetAssignedShard() {
        databaseManagerUtils.cleanUp();
        Shard traditional = new Shard();
        traditional.setType(ShardType.TRADITIONAL);
        traditional.setRouterCanonicalHostname(TestConstants.DEFAULT_SHARD_ROUTER_CANONICAL_HOSTNAME);
        shardDAO.persist(traditional);

        assertThat(shardService.getAssignedShard("myId").getId()).isEqualTo(traditional.getId());
        assertThat(shardService.getAssignedShard("myId").getRouterCanonicalHostname()).isEqualTo(TestConstants.DEFAULT_SHARD_ROUTER_CANONICAL_HOSTNAME);
    }

    @Test
    public void testGetDefaultAssignedShardId() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();

        Shard shard = shardService.getAssignedShard("myId");

        assertThat(shard.getId()).isEqualTo(TestConstants.SHARD_ID);
        assertThat(shard.getRouterCanonicalHostname()).isEqualTo(TestConstants.DEFAULT_SHARD_ROUTER_CANONICAL_HOSTNAME);
    }
}
