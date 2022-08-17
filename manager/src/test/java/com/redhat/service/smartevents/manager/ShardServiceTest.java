package com.redhat.service.smartevents.manager;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.dao.ShardDAO;
import com.redhat.service.smartevents.manager.models.Shard;
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
        Shard shard = new Shard();
        shard.setRouterCanonicalHostname(TestConstants.DEFAULT_SHARD_ROUTER_CANONICAL_HOSTNAME);
        shardDAO.persist(shard);

        Shard retrieved = shardService.getAssignedShard("myId");

        assertThat(retrieved.getId()).isEqualTo(shard.getId());
        assertThat(retrieved.getRouterCanonicalHostname()).isEqualTo(TestConstants.DEFAULT_SHARD_ROUTER_CANONICAL_HOSTNAME);
    }

    @Test
    public void testGetDefaultAssignedShardId() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();

        Shard shard = shardService.getAssignedShard("myId");

        assertThat(shard.getId()).isEqualTo(TestConstants.SHARD_ID);
        assertThat(shard.getRouterCanonicalHostname()).isEqualTo(TestConstants.DEFAULT_SHARD_ROUTER_CANONICAL_HOSTNAME);
    }
}
