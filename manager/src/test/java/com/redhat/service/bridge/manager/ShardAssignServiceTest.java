package com.redhat.service.bridge.manager;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.manager.dao.ShardDAO;
import com.redhat.service.bridge.manager.models.Shard;
import com.redhat.service.bridge.manager.models.ShardType;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ShardAssignServiceTest {

    @Inject
    ShardDAO shardDAO;

    @Inject
    ShardAssignService shardAssignService;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.init();
    }

    @Test
    public void testGetAssignedShardId() {
        Shard traditional = new Shard();
        traditional.setType(ShardType.TRADITIONAL);
        shardDAO.persist(traditional);

        String id = shardAssignService.getAssignedShardId("myId");

        assertThat(id).isEqualTo(traditional.getId());
    }
}
