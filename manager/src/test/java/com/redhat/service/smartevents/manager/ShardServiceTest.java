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
    public void testGetAssignedShardId() {
        databaseManagerUtils.cleanUp();
        Shard shard = new Shard();
        shardDAO.persist(shard);

        String id = shardService.getAssignedShardId("myId");

        assertThat(id).isEqualTo(shard.getId());
    }

    @Test
    public void testGetDefaultAssignedShardId() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();

        String id = shardService.getAssignedShardId("myId");

        assertThat(id).isEqualTo(TestConstants.SHARD_ID);
    }
}
