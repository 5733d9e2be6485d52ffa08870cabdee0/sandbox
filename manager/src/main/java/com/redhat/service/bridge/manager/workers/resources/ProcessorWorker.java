package com.redhat.service.bridge.manager.workers.resources;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.workers.Work;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.vertx.ConsumeEvent;

/*
    Deploys or deletes dependencies for a Processor. If the Processor is using a ManagedConnector as an Action, we deploy the
    Connector, otherwise we immediately succeed.
 */
@ApplicationScoped
public class ProcessorWorker extends AbstractWorker<Processor> {

    @Inject
    private ProcessorDAO processorDAO;

    @Inject
    private ConnectorWorker connectorWorker;

    @Override
    PanacheRepositoryBase<Processor, String> getDao() {
        return processorDAO;
    }

    @Override
    void runCreateOfDependencies(Processor processor) {
        boolean ready = true;

        if (hasManagedConnector(processor)) {
            /*
                If we have to deploy a Managed Connector, delegate to the ConnectorWorker.
             */
            ConnectorEntity connectorEntity = connectorWorker.createDependencies(processor.getConnectorEntities().get(0));
            ready = connectorEntity.getDependencyStatus().isReady();
            if (connectorEntity.getStatus() == BridgeStatus.FAILED) {
                processor.setStatus(BridgeStatus.FAILED);
            } else if (connectorEntity.getStatus() == BridgeStatus.READY) {
                /*
                    If the Connector is ready, we can hand over to the data plane to provision the Processor
                 */
                processor.setStatus(BridgeStatus.PROVISIONING);
            }
        }

        processor.getDependencyStatus().setReady(ready);
    }

    private boolean hasManagedConnector(Processor processor) {
        //TODO - why does this return a list?
        return processor.getConnectorEntities().size() == 1;
    }

    @Override
    void runDeleteOfDependencies(Processor managedResource) {
        boolean deleted = true;
        if (hasManagedConnector(managedResource)) {
            ConnectorEntity connectorEntity = connectorWorker.deleteDependencies(managedResource.getConnectorEntities().get(0));
            deleted = connectorEntity.getDependencyStatus().isDeleted();
        }

        managedResource.getDependencyStatus().setDeleted(deleted);
    }

    @ConsumeEvent(value = "Processor", blocking = true)
    public void handleWork(Work work) {
        super.handleWork(work);
    }
}
