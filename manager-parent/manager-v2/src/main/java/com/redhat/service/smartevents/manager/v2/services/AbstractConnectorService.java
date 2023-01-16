package com.redhat.service.smartevents.manager.v2.services;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.NoQuotaAvailable;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.connectors.ConnectorDefinition;
import com.redhat.service.smartevents.infra.v2.api.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.manager.core.services.ShardService;
import com.redhat.service.smartevents.manager.v2.ams.QuotaConfigurationProvider;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ConnectorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ConnectorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.redhat.service.smartevents.manager.v2.utils.StatusUtilities.getModifiedAt;
import static com.redhat.service.smartevents.manager.v2.utils.StatusUtilities.getStatusMessage;

public abstract class AbstractConnectorService implements ConnectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConnectorService.class);

    @Inject
    BridgeService bridgeService;

    protected abstract ConnectorType getConnectorType();

    protected abstract ConnectorDAO getDAO();

    protected abstract long getOrganisationConnectorsQuota(String organisationId);

    protected abstract List<Condition> createAcceptedConditions();

    @Override
    @Transactional
    public Connector createConnector(String bridgeId, String customerId, String owner, String organisationId, ConnectorRequest connectorRequest) {
        // We cannot deploy Processors to a Bridge that is not available. This throws an Exception if the Bridge is not READY.
        Bridge bridge = bridgeService.getReadyBridge(bridgeId, customerId);

        // Check connectors limits
        long totalConnectors = getDAO().countByBridgeIdAndCustomerId(bridgeId, customerId);
        if (totalConnectors + 1 > getOrganisationConnectorsQuota(organisationId)) {
            throw new NoQuotaAvailable(
                    String.format("There are already '%d' '%s' connectors attached to the bridge '%s': you reached the limit for your organisation settings.", totalConnectors, getConnectorType().serialize(), bridgeId));
        }

        return doCreateConnector(bridge, customerId, owner, connectorRequest);
    }


    private Connector doCreateConnector(Bridge bridge, String customerId, String owner, ConnectorRequest connectorRequest) {
        String bridgeId = bridge.getId();
        if (getDAO().findByBridgeIdAndName(bridgeId, connectorRequest.getName()) != null) {
            throw new AlreadyExistingItemException("Connector with name '" + connectorRequest.getName() + "' already exists for bridge with id '" + bridgeId + "' for customer '" + customerId + "'");
        }

        Operation operation = new Operation();
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));
        operation.setType(OperationType.CREATE);

        Connector connector = connectorRequest.toEntity();
        connector.setOperation(operation);
        connector.setConditions(createAcceptedConditions());
        connector.setOwner(owner);
        connector.setBridge(bridge);
        connector.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        connector.setGeneration(0);

        connector.setConnectorTypeId(connectorRequest.getConnectorTypeId());
        connector.setType(getConnectorType());

        ObjectNode connectorPayload = connectorRequest.getConnector();

        ConnectorDefinition definition = new ConnectorDefinition(connectorPayload);
        connector.setDefinition(definition);

        // Processor and Work should always be created in the same transaction
        getDAO().persist(connector);

        // Schedule worker for connector
        // workManager.schedule(connector);

        // Record metrics
        // metricsService.onOperationStart(connector, MetricsOperation.MANAGER_RESOURCE_PROVISION);

        LOGGER.info("Connector with id '{}' for customer '{}' on bridge '{}' has been marked for creation",
                    connector.getId(),
                    connector.getBridge().getCustomerId(),
                    connector.getBridge().getId());

        return connector;
    }
}
