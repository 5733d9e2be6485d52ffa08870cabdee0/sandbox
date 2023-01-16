package com.redhat.service.smartevents.manager.v2.persistence.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;

@ApplicationScoped
@Transactional
public class SinkConnectorDAO extends ConnectorDAO {

    public SinkConnectorDAO() {
        super(ConnectorType.SINK);
    }
}
