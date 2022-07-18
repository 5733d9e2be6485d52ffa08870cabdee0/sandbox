package com.redhat.service.smartevents.manager.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.manager.models.ConnectorEntity;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@Transactional
@ApplicationScoped
public class ConnectorsDAO implements PanacheRepositoryBase<ConnectorEntity, String> {

    public ConnectorEntity findByProcessorIdAndName(String processorId, String name) {
        Parameters p = Parameters.with(ConnectorEntity.NAME_PARAM, name).and(ConnectorEntity.PROCESSOR_ID_PARAM, processorId);
        return singleResultFromList(find("#CONNECTORENTITY.findByProcessorIdAndName", p));
    }

    public ConnectorEntity findByProcessorId(String processorId) {
        Parameters p = Parameters.with(ConnectorEntity.PROCESSOR_ID_PARAM, processorId);
        return singleResultFromList(find("#CONNECTORENTITY.findByProcessorId", p));
    }

    public List<ConnectorEntity> findConnectorsByProcessorId(String processorId) {
        Parameters p = Parameters.with(ConnectorEntity.PROCESSOR_ID_PARAM, processorId);
        return find("#CONNECTORENTITY.findByProcessorId", p).list();
    }

    private ConnectorEntity singleResultFromList(PanacheQuery<ConnectorEntity> find) {
        List<ConnectorEntity> processors = find.list();
        if (processors.size() > 1) {
            throw new IllegalStateException("Multiple Entities returned from a Query that should only return a single Entity");
        }
        return processors.size() == 1 ? processors.get(0) : null;
    }
}
