package com.redhat.service.smartevents.manager.dao;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.models.Shard;
import com.redhat.service.smartevents.manager.models.ShardType;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ShardDAOTest {

    @Inject
    ShardDAO shardDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void before() {
        databaseManagerUtils.cleanUp();
    }

    @Test
    public void testFindByType() {
        Shard traditional = new Shard();
        traditional.setType(ShardType.TRADITIONAL);

        Shard knative = new Shard();
        knative.setType(ShardType.KNATIVE);

        shardDAO.persist(traditional);
        shardDAO.persist(knative);

        List<Shard> traditionalShards = shardDAO.findByType(ShardType.TRADITIONAL);

        assertThat(traditionalShards.size()).isEqualTo(1);
        assertThat(traditionalShards.get(0).getId()).isEqualTo(traditional.getId());

        List<Shard> knativeShards = shardDAO.findByType(ShardType.KNATIVE);

        assertThat(knativeShards.size()).isEqualTo(1);
        assertThat(knativeShards.get(0).getId()).isEqualTo(knative.getId());
    }
}
