package com.redhat.service.smartevents.manager.v2.services;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions;
import com.redhat.service.smartevents.manager.v2.ams.QuotaConfigurationProvider;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.SourceConnectorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ConnectorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.SourceConnectorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;

@ApplicationScoped
public class SourceConnectorService extends AbstractConnectorService<SourceConnectorResponse> {

    @Inject
    SourceConnectorDAO sourceConnectorDAO;

    @Inject
    QuotaConfigurationProvider quotaConfigurationProvider;

    @Override
    protected ConnectorType getConnectorType() {
        return ConnectorType.SOURCE;
    }

    @Override
    protected ConnectorDAO getDAO() {
        return sourceConnectorDAO;
    }

    @Override
    protected long getOrganisationConnectorsQuota(String organisationId) {
        return quotaConfigurationProvider.getOrganisationQuotas(organisationId).getSourceConnectorsQuota();
    }

    @Override
    protected List<Condition> createAcceptedConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(DefaultConditions.CP_CONNECTOR_READY_NAME, ConditionStatus.UNKNOWN, null, null, null, ComponentType.MANAGER, ZonedDateTime.now(ZoneOffset.UTC)));
        conditions.add(new Condition(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.UNKNOWN, null, null, null, ComponentType.SHARD, ZonedDateTime.now(ZoneOffset.UTC)));
        return conditions;
    }

    @Override
    protected SourceConnectorResponse generateSpecificResponse(Connector connector) {
        SourceConnectorResponse sourceConnectorResponse = new SourceConnectorResponse();
        sourceConnectorResponse.setHref(getHref(connector));
        return sourceConnectorResponse;
    }

    private String getHref(Connector connector) {
        if (Objects.nonNull(connector.getBridge())) {
            return V2APIConstants.V2_USER_API_BASE_PATH + connector.getBridge().getId() + "/sources/" + connector.getId();
        }
        return null;
    }
}
