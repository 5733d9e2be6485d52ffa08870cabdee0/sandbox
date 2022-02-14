package com.redhat.service.bridge.manager.utils;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;
import com.redhat.service.bridge.manager.dao.ShardDAO;
import com.redhat.service.bridge.manager.models.Shard;
import com.redhat.service.bridge.manager.models.ShardType;

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

    /**
     * Until the Processor is "immutable", meaning that it is not possible to add/remove filters dinamically, the processor
     * will cascade the removal of filters that belongs to it.
     * This is why it is not needed to inject the FilterDAO atm.
     */

    /**
     * Clean everything from the DB. Processors must be deleted before bridges.
     */
    @Transactional
    public void init() {
        // Clean up
        deleteAllConnectors();
        deleteAllProcessors();
        deleteAllBridges();
        deleteAllShards();

        // Register defaults
        registerDefaultShard();
    }

    /**
     * processorDAO.deleteAll() does not cascade! https://github.com/quarkusio/quarkus/issues/13941
     */
    private void deleteAllProcessors() {
        List<String> ids = processorDAO.getEntityManager().createQuery("select p.id from Processor p", String.class).getResultList();
        ids.forEach(x -> processorDAO.deleteById(x));
    }

    private void deleteAllBridges() {
        List<String> ids = bridgeDAO.getEntityManager().createQuery("select b.id from Bridge b", String.class).getResultList();
        ids.forEach(x -> bridgeDAO.deleteById(x));
    }

    private void deleteAllConnectors() {
        List<String> ids = connectorsDAO.getEntityManager().createQuery("select c.id from ConnectorEntity c", String.class).getResultList();
        ids.forEach(x -> connectorsDAO.deleteById(x));
    }

    private void deleteAllShards() {
        List<String> ids = shardDAO.getEntityManager().createQuery("select s.id from Shard s", String.class).getResultList();
        ids.forEach(x -> shardDAO.deleteById(x));
    }

    private void registerDefaultShard() {
        Shard traditional = new Shard();
        traditional.setId(TestConstants.SHARD_ID);
        traditional.setType(ShardType.TRADITIONAL);
        shardDAO.persist(traditional);
    }
}
