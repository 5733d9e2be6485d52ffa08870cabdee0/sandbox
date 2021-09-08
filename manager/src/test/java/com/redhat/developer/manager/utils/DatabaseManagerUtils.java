package com.redhat.developer.manager.utils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.developer.manager.dao.BridgeDAO;

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

    /**
     * Completely non performant way to cascade the delete of all bridges to their Processors. Performance
     * doesn't really matter as this is only used in a testing scenario.
     */
    @Transactional
    public void cleanDatabase() {
        bridgeDAO.listAll().stream().forEach(bridgeDAO::delete);
    }
}
