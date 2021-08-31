package com.redhat.developer.manager;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.developer.manager.api.exceptions.AlreadyExistingItemException;
import com.redhat.developer.manager.dao.ConnectorDAO;
import com.redhat.developer.manager.models.Connector;
import com.redhat.developer.manager.models.ConnectorStatus;
import com.redhat.developer.manager.requests.ConnectorRequest;

@ApplicationScoped
public class ConnectorsServiceImpl implements ConnectorsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorsServiceImpl.class);

    @Inject
    ConnectorDAO connectorDAO;

    @Override
    public Connector createConnector(String customerId, ConnectorRequest connectorRequest) {
        if (connectorDAO.findByNameAndCustomerId(connectorRequest.getName(), customerId).size() > 0) {
            throw new AlreadyExistingItemException("Element already present in storage.");
        }

        Connector connector = connectorRequest.toEntity();
        connector.setStatus(ConnectorStatus.REQUESTED);
        connector.setCustomerId(customerId);
        connectorDAO.persist(connector);
        LOGGER.info(String.format("[manager] Connector with id %s has been created", connector.getId()));
        return connector;
    }

    @Override
    public List<Connector> getConnectors(String customerId) {
        // TODO: filter by customerId and add pagination
        return connectorDAO.listAll();
    }

    @Override
    public List<Connector> getConnectorsToDeploy() {
        return connectorDAO.findByStatus(ConnectorStatus.REQUESTED);
    }

    @Override
    @Transactional
    public Connector updateConnector(Connector connector) {
        connectorDAO.getEntityManager().merge(connector);
        LOGGER.info(String.format("[manager] Connector with id %s has been updated", connector.getId()));
        return connector;
    }
}
