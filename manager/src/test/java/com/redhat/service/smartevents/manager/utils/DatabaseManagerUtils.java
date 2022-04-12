package com.redhat.service.smartevents.manager.utils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.manager.TestConstants;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.dao.ConnectorsDAO;
import com.redhat.service.smartevents.manager.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.dao.ShardDAO;
import com.redhat.service.smartevents.manager.dao.WorkDAO;
import com.redhat.service.smartevents.manager.models.Shard;
import com.redhat.service.smartevents.manager.models.ShardType;

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
    ConnectorsDAO connectorsDAO;

    @Inject
    ShardDAO shardDAO;

    @Inject
    WorkDAO workDAO;

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
        deleteAllWork();
        deleteAllConnectors();
        deleteAllProcessors();
        deleteAllBridges();
        deleteAllShards();
    }

    private void deleteAllProcessors() {
        processorDAO.getEntityManager().createQuery("DELETE FROM Processor").executeUpdate();
    }

    private void deleteAllBridges() {
        bridgeDAO.getEntityManager().createQuery("DELETE FROM Bridge").executeUpdate();
    }

    private void deleteAllWork() {
        workDAO.getEntityManager().createQuery("DELETE FROM Work").executeUpdate();
    }

    private void deleteAllConnectors() {
        connectorsDAO.getEntityManager().createQuery("DELETE FROM ConnectorEntity").executeUpdate();
    }

    private void deleteAllShards() {
        shardDAO.getEntityManager().createQuery("DELETE FROM Shard").executeUpdate();
    }

    private void registerDefaultShard() {
        Shard traditional = new Shard();
        traditional.setId(TestConstants.SHARD_ID);
        traditional.setType(ShardType.TRADITIONAL);
        shardDAO.persist(traditional);
    }
}
