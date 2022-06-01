package com.redhat.service.smartevents.manager.connectors;

import java.time.ZonedDateTime;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.dao.ConnectorsDAO;
import com.redhat.service.smartevents.manager.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.GatewayConnector;
import com.redhat.service.smartevents.processor.ProcessorCatalogService;

@ApplicationScoped
public class ConnectorsServiceImpl implements ConnectorsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorsServiceImpl.class);

    @Inject
    ConnectorsDAO connectorsDAO;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Inject
    GatewayConfiguratorService gatewayConfiguratorService;

    @Inject
    GatewayConnector gatewayConnector;

    @Inject
    ProcessorCatalogService processorCatalogService;

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    // Connector should always be marked for creation in the same transaction as a Processor
    public void createConnectorEntity(Processor processor) {
        if (processor.getType() == ProcessorType.SOURCE) {
            createConnectorEntity(processor, processor.getDefinition().getRequestedSource());
        } else {
            createConnectorEntity(processor, processor.getDefinition().getRequestedAction());
        }
    }

    @Transactional(Transactional.TxType.MANDATORY)
    private void createConnectorEntity(Processor processor, Action action) {
        if (!processorCatalogService.isConnector(ProcessorType.SINK, action.getType())) {
            return;
        }
        String topicName = gatewayConfiguratorService.getConnectorTopicName(processor.getId());
        persistConnectorEntity(processor, topicName, ConnectorType.SINK, action.getType(), gatewayConnector.connectorPayload(action, topicName));
    }

    @Transactional(Transactional.TxType.MANDATORY)
    // Connector should always be marked for creation in the same transaction as a Processor
    public void createConnectorEntity(Processor processor, Source source) {
        if (!processorCatalogService.isConnector(ProcessorType.SOURCE, source.getType())) {
            return;
        }
        String topicName = gatewayConfiguratorService.getConnectorTopicName(processor.getId());
        persistConnectorEntity(processor, topicName, ConnectorType.SOURCE, source.getType(), gatewayConnector.connectorPayload(source, topicName));
    }

    private void persistConnectorEntity(Processor processor, String topicName, ConnectorType connectorType, String connectorTypeId, JsonNode connectorPayload) {
        String newConnectorName = resourceNamesProvider.getProcessorConnectorName(processor.getId());

        ConnectorEntity newConnectorEntity = new ConnectorEntity();

        newConnectorEntity.setType(connectorType);
        newConnectorEntity.setName(newConnectorName);
        newConnectorEntity.setStatus(ManagedResourceStatus.ACCEPTED);
        newConnectorEntity.setSubmittedAt(ZonedDateTime.now());
        newConnectorEntity.setProcessor(processor);
        newConnectorEntity.setTopicName(topicName);
        newConnectorEntity.setConnectorTypeId(connectorTypeId);
        newConnectorEntity.setDefinition(connectorPayload);

        connectorsDAO.persist(newConnectorEntity);
    }

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    // Connector should always be marked for destruction in the same transaction as a Processor
    public void deleteConnectorEntity(Processor processor) {
        Optional.ofNullable(connectorsDAO.findByProcessorId(processor.getId())).ifPresent(c -> c.setStatus(ManagedResourceStatus.DEPROVISION));
    }
}
