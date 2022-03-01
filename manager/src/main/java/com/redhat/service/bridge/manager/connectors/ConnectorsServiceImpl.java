package com.redhat.service.bridge.manager.connectors;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ConnectorStatus;
import com.redhat.service.bridge.manager.actions.connectors.ConnectorAction;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;

@ApplicationScoped
public class ConnectorsServiceImpl implements ConnectorsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorsServiceImpl.class);

    @Inject
    ConnectorsDAO connectorsDAO;

    @Override
    @Transactional(Transactional.TxType.MANDATORY) // Connector should always be created in the same transaction of a Processor
    public Optional<ConnectorEntity> createConnectorEntity(BaseAction resolvedAction,
            Processor processor,
            ActionProvider actionProvider) {

        if (!actionProvider.isConnectorAction()) {
            return Optional.empty();
        }

        ConnectorAction connectorAction = (ConnectorAction) actionProvider;
        JsonNode connectorPayload = connectorAction.connectorPayload(resolvedAction);

        String connectorType = connectorAction.getConnectorType();
        String newConnectorName = connectorName(connectorType, processor);
        String topicName = connectorAction.topicName(resolvedAction);

        ConnectorEntity newConnectorEntity = new ConnectorEntity();

        newConnectorEntity.setName(newConnectorName);
        newConnectorEntity.setStatus(ConnectorStatus.ACCEPTED);
        newConnectorEntity.setDesiredStatus(ConnectorStatus.READY);
        newConnectorEntity.setSubmittedAt(ZonedDateTime.now());
        newConnectorEntity.setProcessor(processor);
        newConnectorEntity.setDefinition(connectorPayload);
        newConnectorEntity.setConnectorType(connectorType);
        newConnectorEntity.setTopicName(topicName);

        connectorsDAO.persist(newConnectorEntity);

        return Optional.of(newConnectorEntity);
    }

    @Override
    @Transactional
    public List<ConnectorEntity> deleteConnectorIfNeeded(Processor processor) {

        List<ConnectorEntity> optionalConnector = connectorsDAO.findByProcessorId(processor.getId());

        return optionalConnector.stream().peek(c -> {
            String connectorId = c.getId();
            c.setDesiredStatus(ConnectorStatus.DELETED);
            LOGGER.info("connector with id '{}' has been marked for deletion", connectorId);
        }).collect(Collectors.toList());
    }

    private String connectorName(String connectorType, Processor processor) {
        return String.format("OpenBridge-%s-%s", connectorType, processor.getId());
    }
}
