package com.redhat.service.bridge.manager.utils;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.Processor;

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
        deleteAllProcessors();
        deleteAllBridges();
    }

    /**
     * processorDAO.deleteAll() does not cascade! https://github.com/quarkusio/quarkus/issues/13941
     */
    private void deleteAllProcessors() {
        List<Processor> processors = processorDAO.listAll();
        processors.forEach(x -> processorDAO.deleteById(x.getId()));
    }

    private void deleteAllBridges() {
        List<Bridge> bridges = bridgeDAO.listAll();
        bridges.forEach(x -> bridgeDAO.deleteById(x.getId()));
    }
}
