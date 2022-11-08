package com.redhat.service.smartevents.manager.core.utils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.manager.core.TestConstants;
import com.redhat.service.smartevents.manager.core.persistence.dao.ShardDAO;
import com.redhat.service.smartevents.manager.core.persistence.models.Shard;

/**
 * This bean must be injected in every test class that uses the database.
 * In addition to that and the `cleanDatabase` method must be call `@BeforeEach` test.
 */
@ApplicationScoped
public class DatabaseManagerUtils {

    @Inject
    ShardDAO shardDAO;

    /**
     * Clean everything from the DB and init with default shard.
     */
    @Transactional
    public void cleanUpAndInitWithDefaultShard() {
        // Clean up
        cleanUp();

        // Register defaults
        registerDefaultShard();
    }

    /**
     * Clean everything from the DB.
     */
    @Transactional
    public void cleanUp() {
        // Clean up
        deleteAllShards();
    }

    private void deleteAllShards() {
        shardDAO.getEntityManager().createQuery("DELETE FROM Shard").executeUpdate();
    }

    private void registerDefaultShard() {
        Shard shard = new Shard();
        shard.setId(TestConstants.SHARD_ID);
        shard.setRouterCanonicalHostname(TestConstants.DEFAULT_SHARD_ROUTER_CANONICAL_HOSTNAME);
        shardDAO.persist(shard);
    }
}
