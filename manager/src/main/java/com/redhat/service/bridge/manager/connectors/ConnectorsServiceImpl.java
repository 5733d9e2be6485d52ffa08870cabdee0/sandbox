package com.redhat.service.bridge.manager.connectors;

import java.time.ZonedDateTime;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.providers.ResourceNamesProvider;
import com.redhat.service.bridge.processor.actions.ActionConnector;
import com.redhat.service.bridge.processor.actions.ActionConnectorFactory;
import com.redhat.service.bridge.processor.actions.ActionService;

@ApplicationScoped
public class ConnectorsServiceImpl implements ConnectorsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorsServiceImpl.class);

    @Inject
    ConnectorsDAO connectorsDAO;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Inject
    ActionService actionService;

    @Inject
    ActionConnectorFactory actionConnectorFactory;

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    // Connector should always be marked for creation in the same transaction as a Processor
    public void createConnectorEntity(Processor processor, BaseAction action) {
        if (!actionConnectorFactory.hasConnector(action.getType())) {
            return;
        }

        String topicName = actionService.getConnectorTopicName(processor.getId());

        ActionConnector actionConnector = actionConnectorFactory.get(action.getType());
        JsonNode connectorPayload = actionConnector.connectorPayload(action, topicName);

        String connectorType = actionConnector.getConnectorType();
        String newConnectorName = resourceNamesProvider.getProcessorConnectorName(processor.getId());

        ConnectorEntity newConnectorEntity = new ConnectorEntity();

        newConnectorEntity.setName(newConnectorName);
        newConnectorEntity.setStatus(ManagedResourceStatus.ACCEPTED);
        newConnectorEntity.setSubmittedAt(ZonedDateTime.now());
        newConnectorEntity.setProcessor(processor);
        newConnectorEntity.setDefinition(connectorPayload);
        newConnectorEntity.setConnectorType(connectorType);
        newConnectorEntity.setTopicName(topicName);

        connectorsDAO.persist(newConnectorEntity);
    }

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    // Connector should always be marked for destruction in the same transaction as a Processor
    public void deleteConnectorEntity(Processor processor) {
        Optional.ofNullable(connectorsDAO.findByProcessorId(processor.getId())).ifPresent(c -> c.setStatus(ManagedResourceStatus.DEPROVISION));
    }
}
