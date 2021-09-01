package com.redhat.developer.manager.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.developer.manager.models.Connector;
import com.redhat.developer.manager.models.ConnectorStatus;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@ApplicationScoped
@Transactional
public class ConnectorDAO implements PanacheRepositoryBase<Connector, String> {

    public List<Connector> findByStatus(ConnectorStatus status) {
        Parameters params = Parameters
                .with("status", status);
        return find("#CONNECTOR.findByStatus", params).list();
    }

    public Connector findByNameAndCustomerId(String name, String customerId) {
        Parameters params = Parameters
                .with("name", name).and("customerId", customerId);
        return find("#CONNECTOR.findByNameAndCustomerId", params).firstResult();
    }
}
