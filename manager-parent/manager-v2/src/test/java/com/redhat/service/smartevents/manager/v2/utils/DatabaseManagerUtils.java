package com.redhat.service.smartevents.manager.v2.utils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.manager.core.persistence.dao.ShardDAO;
import com.redhat.service.smartevents.manager.core.persistence.models.Shard;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ConditionDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.OperationDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ProcessorDAO;

/**
 * This bean must be injected in every test class that uses the database.
 * In addition to that and the `cleanDatabase` method must be call `@BeforeEach` test.
 */
@ApplicationScoped
public class DatabaseManagerUtils {

    /**
     * Inject all the DAOs of the application
     */
    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    OperationDAO operationDAO;

    @Inject
    ConditionDAO conditionDAO;

    @Inject
    ShardDAO shardDAO;

    /**
     * Until the Processor is "immutable", meaning that it is not possible to add/remove filters dynamically, the processor
     * will cascade the removal of filters that belongs to it.
     * This is why it is not needed to inject the FilterDAO atm.
     */

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
        deleteAllConditions();
        deleteAllOperations();
        deleteAllProcessors();
        deleteAllBridges();
        deleteAllShards();
    }

    private void deleteAllProcessors() {
        processorDAO.getEntityManager().createQuery("DELETE FROM Processor_V2").executeUpdate();
    }

    private void deleteAllBridges() {
        bridgeDAO.getEntityManager().createQuery("DELETE FROM Bridge_V2").executeUpdate();
    }

    private void deleteAllConditions() {
        bridgeDAO.getEntityManager().createQuery("DELETE FROM Condition").executeUpdate();
    }

    private void deleteAllOperations() {
        bridgeDAO.getEntityManager().createQuery("DELETE FROM Operation").executeUpdate();
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
