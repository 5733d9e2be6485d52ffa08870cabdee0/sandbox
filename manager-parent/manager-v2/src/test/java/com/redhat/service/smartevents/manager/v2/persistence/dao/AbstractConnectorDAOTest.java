package com.redhat.service.smartevents.manager.v2.persistence.dao;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.Fixtures;

import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridge;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createCondition;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createConnector;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractConnectorDAOTest {

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    public abstract ConnectorDAO getConnectorDAO();

    public abstract ConnectorType getConnectorType();

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
    }

    @Test
    @Transactional
    public void testStoreConnector() {
        Bridge bridge = createBridge();
        bridgeDAO.persist(bridge);

        Connector connector = Fixtures.createConnector(bridge, getConnectorType());
        getConnectorDAO().persist(connector);

        Connector retrieved = getConnectorDAO().findByIdWithConditions(connector.getId());

        assertThat(retrieved.getId()).isEqualTo(connector.getId());
        assertThat(retrieved.getConnectorExternalId()).isEqualTo(TestConstants.DEFAULT_CONNECTOR_EXTERNAL_ID);
        assertThat(retrieved.getConnectorTypeId()).isEqualTo(TestConstants.DEFAULT_CONNECTOR_TYPE_ID);
        assertThat(retrieved.getTopicName()).isEqualTo(TestConstants.DEFAULT_CONNECTOR_TOPIC_NAME);
        assertThat(retrieved.getTopicName()).isEqualTo(TestConstants.DEFAULT_CONNECTOR_TOPIC_NAME);
        assertThat(retrieved.getOperation()).isNotNull();
        assertThat(retrieved.getOperation().getType()).isNotNull();
        assertThat(retrieved.getOperation().getRequestedAt()).isNotNull();
        assertThat(retrieved.getConditions()).isNull();
        assertThat(retrieved.getError()).isNull();
    }

    @Test
    @Transactional
    public void testStoreConnectorWithConditions() {
        Bridge bridge = createBridge();
        bridgeDAO.persist(bridge);

        Connector connector = Fixtures.createReadyConnector(bridge, getConnectorType());
        getConnectorDAO().persist(connector);

        Connector retrieved = getConnectorDAO().findByIdWithConditions(connector.getId());

        assertThat(retrieved.getId()).isEqualTo(connector.getId());
        assertThat(retrieved.getConditions()).hasSize(2);
    }

    @Test
    @Transactional
    public void findByShardIdToDeployOrDeleteWhenControlPlaneIsComplete() {
        Bridge b = createBridge();
        Connector c = createConnector(b, getConnectorType());
        bridgeDAO.persist(b);

        Condition condition1 = createCondition();
        Condition condition2 = createCondition();
        Condition condition3 = createCondition();
        condition3.setComponent(ComponentType.SHARD);
        condition3.setStatus(ConditionStatus.UNKNOWN);

        c.setConditions(List.of(condition1, condition2, condition3));
        getConnectorDAO().persist(c);

        List<Connector> connectors = getConnectorDAO().findByShardIdToDeployOrDelete(b.getShardId());
        assertThat(connectors).isNotNull();
        assertThat(connectors).hasSize(1);
        assertThat(connectors.get(0).getId()).isEqualTo(c.getId());
    }

    @Test
    @Transactional
    public void findByShardIdToDeployOrDeleteWhenControlPlaneIsIncomplete() {
        Bridge b = createBridge();
        Connector c = createConnector(b, getConnectorType());
        bridgeDAO.persist(b);

        Condition condition1 = createCondition();
        Condition condition2 = createCondition();
        condition2.setStatus(ConditionStatus.UNKNOWN);

        Condition condition3 = createCondition();
        condition3.setComponent(ComponentType.SHARD);
        condition3.setStatus(ConditionStatus.UNKNOWN);

        c.setConditions(List.of(condition1, condition2, condition3));
        getConnectorDAO().persist(c);

        List<Connector> processors = getConnectorDAO().findByShardIdToDeployOrDelete(b.getShardId());
        assertThat(processors).isNotNull();
        assertThat(processors).isEmpty();
    }

    @Test
    @Transactional
    public void testTypeDiscriminator() {
        Bridge b = createBridge();
        Connector connector = Fixtures.createConnector(b, ConnectorType.SOURCE.equals(getConnectorType()) ? ConnectorType.SINK : ConnectorType.SOURCE);
        bridgeDAO.persist(b);

        Condition condition1 = createCondition();
        Condition condition2 = createCondition();
        Condition condition3 = createCondition();
        condition3.setComponent(ComponentType.SHARD);
        condition3.setStatus(ConditionStatus.UNKNOWN);

        connector.setConditions(List.of(condition1, condition2, condition3));
        getConnectorDAO().persist(connector);

        assertThat(getConnectorDAO().findByShardIdToDeployOrDelete(b.getShardId())).hasSize(0);
        assertThat(getConnectorDAO().findByIdWithConditions(connector.getId())).isNull();
    }
}
