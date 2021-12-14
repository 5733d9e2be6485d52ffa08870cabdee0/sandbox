package com.redhat.service.bridge.manager.utils;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;

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

    /**
     * Until the Processor is "immutable", meaning that it is not possible to add/remove filters dinamically, the processor
     * will cascade the removal of filters that belongs to it.
     * This is why it is not needed to inject the FilterDAO atm.
     */

    /**
     * Clean everything from the DB. Processors must be deleted before bridges.
     */
    @Transactional
    public void cleanDatabase() {
        deleteAllConnectors();
        deleteAllProcessors();
        deleteAllBridges();
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
}
