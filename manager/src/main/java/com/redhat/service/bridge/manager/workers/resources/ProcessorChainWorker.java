package com.redhat.service.bridge.manager.workers.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.models.Work;
import com.redhat.service.bridge.manager.workers.chain.Link;
import com.redhat.service.bridge.manager.workers.chain.LinkImpl;

import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class ProcessorChainWorker extends AbstractChainWorker<Processor> {

    @Inject
    ProcessorWorker processorWorker;

    @Inject
    ConnectorWorker connectorWorker;

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    ConnectorsDAO connectorsDAO;

    @Override
    // This must be equal to the Processor.class.getName()
    @ConsumeEvent(value = "com.redhat.service.bridge.manager.models.Processor", blocking = true)
    public boolean handleWork(Work work) {
        return super.handleWork(work);
    }

    @Override
    @Transactional
    public List<Link<?>> getLinks(Work work) {
        List<Link<?>> links = new ArrayList<>();

        // Processor work
        Processor processor = processorDAO.findById(work.getManagedResourceId());
        if (Objects.isNull(processor)) {
            String message = String.format("Resource of type '%s' with id '%s' no longer exists in the database.", work.getType(), work.getManagedResourceId());
            throw new IllegalStateException(message);
        }
        links.add(new LinkImpl<>(processorWorker, processor));

        // Connector work
        ConnectorEntity connectorEntity = connectorsDAO.findByProcessorId(work.getManagedResourceId());
        if (Objects.nonNull(connectorEntity)) {
            links.add(new LinkImpl<>(connectorWorker, connectorEntity));
        }

        return links;
    }

}
