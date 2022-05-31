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
import com.redhat.service.smartevents.processor.GatewayConfigurator;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.GatewayConnector;

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
    GatewayConfigurator gatewayConfigurator;

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    // Connector should always be marked for creation in the same transaction as a Processor
    public void createConnectorEntity(Processor processor) {
        if (processor.getType() == ProcessorType.SOURCE) {
            createConnectorEntity(processor, processor.getDefinition().getRequestedSource());
        } else if (processor.getType() == ProcessorType.SINK) {
            createConnectorEntity(processor, processor.getDefinition().getRequestedAction());
        }
    }

    @Transactional(Transactional.TxType.MANDATORY)
    protected void createConnectorEntity(Processor processor, Action action) {
        Optional<GatewayConnector<Action>> optActionConnector = gatewayConfigurator.getActionConnector(action.getType());
        if (optActionConnector.isEmpty()) {
            return;
        }
        String topicName = gatewayConfiguratorService.getConnectorTopicName(processor.getId());
        String errorHandlerTopicName = resourceNamesProvider.getBridgeErrorTopicName(processor.getBridge().getId());
        GatewayConnector<Action> actionConnector = optActionConnector.get();
        persistConnectorEntity(processor,
                topicName,
                actionConnector.getConnectorType(),
                actionConnector.getConnectorTypeId(),
                actionConnector.connectorPayload(action, topicName, errorHandlerTopicName));
    }

    @Transactional(Transactional.TxType.MANDATORY)
    // Connector should always be marked for creation in the same transaction as a Processor
    protected void createConnectorEntity(Processor processor, Source source) {
        GatewayConnector<Source> sourceConnector = gatewayConfigurator.getSourceConnector(source.getType());
        String topicName = gatewayConfiguratorService.getConnectorTopicName(processor.getId());
        String errorHandlerTopicName = resourceNamesProvider.getBridgeErrorTopicName(processor.getBridge().getId());
        persistConnectorEntity(processor,
                topicName,
                sourceConnector.getConnectorType(),
                sourceConnector.getConnectorTypeId(),
                sourceConnector.connectorPayload(source, topicName, errorHandlerTopicName));
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
