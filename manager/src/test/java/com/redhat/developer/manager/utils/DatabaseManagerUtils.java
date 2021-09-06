package com.redhat.developer.manager.utils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
     * Call the `deleteAll` method of all the DAOs injected so to clean up the database entirely.
     */
    public void cleanDatabase() {
        bridgeDAO.deleteAll();
    }
}
