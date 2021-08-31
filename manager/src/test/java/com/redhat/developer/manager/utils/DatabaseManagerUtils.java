package com.redhat.developer.manager.utils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.developer.manager.dao.ConnectorDAO;

@ApplicationScoped
public class DatabaseManagerUtils {

    @Inject
    ConnectorDAO connectorDAO;

    public void cleanDatabase() {
        System.out.println("CALLED");
        connectorDAO.deleteAll();
    }
}
