package com.redhat.service.smartevents.manager.connectors;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.infra.models.VaultSecret;
import com.redhat.service.smartevents.manager.vault.VaultService;

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

    @Inject
    ConnectorsDAO connectorsDAO;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Inject
    GatewayConfiguratorService gatewayConfiguratorService;

    @Inject
    GatewayConfigurator gatewayConfigurator;

    @Inject
    VaultService vaultService;

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

    private VaultSecret getSensitiveParameters(Processor processor) {
        return processor.getVaultReference() == null ? null : vaultService.get(processor.getVaultReference()).await().atMost(Duration.of(5, ChronoUnit.SECONDS));
    }

    private JsonNode createConnectorPayload(Processor processor, ConnectorEntity connectorEntity, Action action) {
        GatewayConnector<Action> gatewayConnector = gatewayConfigurator.getActionConnector(action.getType()).get();
        Optional<VaultSecret> vaultSecret = Optional.ofNullable(getSensitiveParameters(processor));
        return gatewayConnector.connectorPayload(action, connectorEntity.getTopicName(), vaultSecret);
    }

    private JsonNode createConnectorPayload(Processor processor, ConnectorEntity connectorEntity, Source source) {
        GatewayConnector<Source> gatewayConnector = gatewayConfigurator.getSourceConnector(source.getType());
        Optional<VaultSecret> vaultSecret = Optional.ofNullable(getSensitiveParameters(processor));
        return gatewayConnector.connectorPayload(source, connectorEntity.getTopicName(), vaultSecret);
    }

    @Transactional
    @Override
    public JsonNode createConnectorDefinition(String connectorId) {
        ConnectorEntity connector = connectorsDAO.findById(connectorId);
        Processor processor = connector.getProcessor();

        if (processor.getType() == ProcessorType.SINK) {
            return createConnectorPayload(processor, connector, processor.getDefinition().getResolvedAction());
        }

        return createConnectorPayload(processor, connector, processor.getDefinition().getRequestedSource());
    }

    private void createConnectorEntity(Processor processor, Action action) {
        Optional<GatewayConnector<Action>> optActionConnector = gatewayConfigurator.getActionConnector(action.getType());
        if (optActionConnector.isEmpty()) {
            return;
        }
        String topicName = gatewayConfiguratorService.getConnectorTopicName(processor.getId());
        GatewayConnector<Action> actionConnector = optActionConnector.get();
        persistConnectorEntity(processor, topicName, actionConnector.getConnectorType(), actionConnector.getConnectorTypeId());
    }

    private void createConnectorEntity(Processor processor, Source source) {
        GatewayConnector<Source> sourceConnector = gatewayConfigurator.getSourceConnector(source.getType());
        String topicName = gatewayConfiguratorService.getConnectorTopicName(processor.getId());
        persistConnectorEntity(processor, topicName, sourceConnector.getConnectorType(), sourceConnector.getConnectorTypeId());
    }

    private void persistConnectorEntity(Processor processor, String topicName, ConnectorType connectorType, String connectorTypeId) {
        String newConnectorName = resourceNamesProvider.getProcessorConnectorName(processor.getId());

        ConnectorEntity newConnectorEntity = new ConnectorEntity();

        newConnectorEntity.setType(connectorType);
        newConnectorEntity.setName(newConnectorName);
        newConnectorEntity.setStatus(ManagedResourceStatus.ACCEPTED);
        newConnectorEntity.setSubmittedAt(ZonedDateTime.now());
        newConnectorEntity.setProcessor(processor);
        newConnectorEntity.setTopicName(topicName);
        newConnectorEntity.setConnectorTypeId(connectorTypeId);

        connectorsDAO.persist(newConnectorEntity);
    }

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    // Connector should always be marked for destruction in the same transaction as a Processor
    public void deleteConnectorEntity(Processor processor) {
        Optional.ofNullable(connectorsDAO.findByProcessorId(processor.getId())).ifPresent(c -> c.setStatus(ManagedResourceStatus.DEPROVISION));
    }
}
