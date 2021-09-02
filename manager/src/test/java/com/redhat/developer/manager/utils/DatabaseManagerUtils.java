package com.redhat.developer.manager.utils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.developer.manager.dao.ConnectorDAO;

@ApplicationScoped
public class DatabaseManagerUtils {

    @Inject
    ConnectorDAO connectorDAO;

    /**
     *   In this method there should be all the DAOs so to clean up the database entirely.
     *
     *   This is needed because we have tests with restassured + data stored in the database and @TestTransaction does not
     *   rollback the transaction on server side: the database must be cleaned up manually. See https://github.com/quarkusio/quarkus/issues/15436
     */
    public void cleanDatabase() {
        connectorDAO.deleteAll();
    }
}
