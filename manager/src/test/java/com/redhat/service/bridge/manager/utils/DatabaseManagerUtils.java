package com.redhat.service.bridge.manager.utils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.service.bridge.manager.dao.BridgeDAO;
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

    /**
     * Clean everything from the DB. Processors must be deleted before bridges.
     */
    @Transactional
    public void cleanDatabase() {
        processorDAO.deleteAll();
        bridgeDAO.deleteAll();
    }
}
