package com.redhat.service.smartevents.manager.v2.persistence.dao;

import java.util.List;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.Fixtures;

import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.ACCEPTED;
import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.READY;
import static com.redhat.service.smartevents.infra.v2.api.models.queries.QueryFilterInfo.QueryFilterInfoBuilder.filter;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CONNECTOR_ID;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridge;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createCondition;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createConnector;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createConnectorAcceptedConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createConnectorReadyConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createReadyConnector;
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
        assertThat(retrieved.getType()).isEqualTo(getConnectorType());
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

    // Test that all the queries on the Connector use the discriminator: the SinkConnectorDAO should not retrieve Source connectors and viceversa.
    @Test
    @Transactional
    public void testTypeDiscriminator() {
        Bridge b = createBridge();
        // If the DAO is for the Sink, it creates a Source. If the DAO is for the Source, it creates a Sink.
        Connector connector = Fixtures.createConnector(b, ConnectorType.SOURCE.equals(getConnectorType()) ? ConnectorType.SINK : ConnectorType.SOURCE);
        bridgeDAO.persist(b);

        Condition condition1 = createCondition();
        Condition condition2 = createCondition();
        Condition condition3 = createCondition();
        condition3.setComponent(ComponentType.SHARD);
        condition3.setStatus(ConditionStatus.UNKNOWN);

        connector.setConditions(List.of(condition1, condition2, condition3));
        getConnectorDAO().persist(connector);

        // Check that no connectors are found, i.e. all the queries filter by type (the discriminator).
        assertThat(getConnectorDAO().findByShardIdToDeployOrDelete(b.getShardId())).hasSize(0);
        assertThat(getConnectorDAO().findByIdWithConditions(connector.getId())).isNull();
        assertThat(getConnectorDAO().findByIdBridgeIdAndCustomerId(b.getId(), connector.getId(), b.getCustomerId())).isNull();
    }

    @Test
    @Transactional
    public void findByBridgeIdAndName() {
        Bridge b = createBridge();
        Connector c = createConnector(b, getConnectorType());
        bridgeDAO.persist(b);
        getConnectorDAO().persist(c);

        Connector byBridgeIdAndName = getConnectorDAO().findByBridgeIdAndName(b.getId(), c.getName());
        assertThat(byBridgeIdAndName).isNotNull();
        assertThat(byBridgeIdAndName.getName()).isEqualTo(c.getName());
        assertThat(byBridgeIdAndName.getBridge().getId()).isEqualTo(b.getId());
    }

    @Test
    @Transactional
    public void findByBridgeIdAndNameWhenBridgeIdDoesNotMatch() {
        Bridge b = createBridge();
        Connector c = createConnector(b, getConnectorType());
        bridgeDAO.persist(b);
        getConnectorDAO().persist(c);

        assertThat(getConnectorDAO().findByBridgeIdAndName("doesNotExist", c.getName())).isNull();
    }

    @Test
    @Transactional
    public void findByBridgeIdAndNameWhenNameDoesNotMatch() {
        Bridge b = createBridge();
        Connector c = createConnector(b, getConnectorType());
        bridgeDAO.persist(b);
        getConnectorDAO().persist(c);

        assertThat(getConnectorDAO().findByBridgeIdAndName(b.getId(), "doesNotExist")).isNull();
    }

    @Test
    @Transactional
    public void findByBridgeIdAndCustomerId() {
        Bridge b = createBridge();
        Connector c1 = createConnector(b, getConnectorType(), DEFAULT_CONNECTOR_ID + "1", "bar");
        Connector c2 = createConnector(b, getConnectorType(), DEFAULT_CONNECTOR_ID + "2", "foo");
        bridgeDAO.persist(b);
        getConnectorDAO().persist(c1);
        getConnectorDAO().persist(c2);

        ListResult<Connector> listResult = getConnectorDAO().findByBridgeIdAndCustomerId(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, new QueryResourceInfo(0, 100));
        assertThat(listResult.getPage()).isZero();
        assertThat(listResult.getSize()).isEqualTo(2L);
        assertThat(listResult.getTotal()).isEqualTo(2L);

        listResult.getItems().forEach((px) -> assertThat(px.getId()).isIn(c1.getId(), c2.getId()));
    }

    @Test
    @Transactional
    public void findByBridgeIdAndCustomerIdWhenNoConnectorsExist() {
        Bridge b = createBridge();
        bridgeDAO.persist(b);

        ListResult<Connector> listResult = getConnectorDAO().findByBridgeIdAndCustomerId(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, new QueryResourceInfo(0, 100));
        assertThat(listResult.getPage()).isZero();
        assertThat(listResult.getSize()).isZero();
        assertThat(listResult.getTotal()).isZero();
    }

    @Test
    @Transactional
    public void findByBridgeIdAndCustomerId_pageOffset() {
        Bridge b = createBridge();
        Connector c1 = createConnector(b, getConnectorType(), DEFAULT_CONNECTOR_ID + "1", "bar");
        Connector c2 = createConnector(b, getConnectorType(), DEFAULT_CONNECTOR_ID + "2", "foo");
        bridgeDAO.persist(b);
        getConnectorDAO().persist(c1);
        getConnectorDAO().persist(c2);

        ListResult<Connector> listResult = getConnectorDAO().findByBridgeIdAndCustomerId(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, new QueryResourceInfo(1, 1));
        assertThat(listResult.getPage()).isEqualTo(1L);
        assertThat(listResult.getSize()).isEqualTo(1L);
        assertThat(listResult.getTotal()).isEqualTo(2L);

        // Results are sorted descending by default. The last page, 1, will contain the first processor.
        assertThat(listResult.getItems().get(0).getId()).isEqualTo(c1.getId());
    }

    @Test
    @Transactional
    public void testCountByBridgeIdAndCustomerId() {
        Bridge b = createBridge();
        Connector c1 = createConnector(b, getConnectorType(), DEFAULT_CONNECTOR_ID + "1", "bar");
        Connector c2 = createConnector(b, getConnectorType(), DEFAULT_CONNECTOR_ID + "2", "foo");
        bridgeDAO.persist(b);
        getConnectorDAO().persist(c1);
        getConnectorDAO().persist(c2);

        long total = getConnectorDAO().countByBridgeIdAndCustomerId(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(total).isEqualTo(2L);
    }

    @Test
    @Transactional
    public void findByIdBridgeIdAndCustomerId() {
        Bridge b = createBridge();
        Connector c = createReadyConnector(b, getConnectorType());
        bridgeDAO.persist(b);
        getConnectorDAO().persist(c);

        Connector found = getConnectorDAO().findByIdBridgeIdAndCustomerId(b.getId(), c.getId(), b.getCustomerId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(found.getId());
        assertThat(found.getConditions()).isNotNull();
    }

    @Test
    @Transactional
    public void findByIdBridgeIdAndCustomerId_doesNotExist() {
        Bridge b = createBridge();
        Connector c = createConnector(b, getConnectorType());
        bridgeDAO.persist(b);
        getConnectorDAO().persist(c);

        Connector found = getConnectorDAO().findByIdBridgeIdAndCustomerId(b.getId(), "doesntExist", b.getCustomerId());
        assertThat(found).isNull();
    }

    @Test
    @Transactional
    void testGetProcessorsFilterByName() {
        Bridge b = createBridge();
        Connector c1 = createConnector(b, getConnectorType(), DEFAULT_CONNECTOR_ID + "1", "bar");
        Connector c2 = createConnector(b, getConnectorType(), DEFAULT_CONNECTOR_ID + "2", "foo");
        bridgeDAO.persist(b);
        getConnectorDAO().persist(c1);
        getConnectorDAO().persist(c2);

        ListResult<Connector> results = getConnectorDAO().findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryResourceInfo(0, 100, filter().by(c1.getName()).build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);
        assertThat(results.getItems().get(0).getId()).isEqualTo(c1.getId());
    }

    @Test
    @Transactional
    public void testGetProcessorsFilterByNameWildcard() {
        Bridge b = createBridge();
        Connector c1 = createConnector(b, getConnectorType(), DEFAULT_CONNECTOR_ID + "1", "foo");
        Connector c2 = createConnector(b, getConnectorType(), DEFAULT_CONNECTOR_ID + "2", "foo2");
        bridgeDAO.persist(b);
        getConnectorDAO().persist(c1);
        getConnectorDAO().persist(c2);

        ListResult<Connector> results = getConnectorDAO().findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryResourceInfo(0, 100, filter().by("foo").build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(2L);
        assertThat(results.getTotal()).isEqualTo(2L);

        // Results are sorted descending by default
        assertThat(results.getItems().get(0).getId()).isEqualTo(c2.getId());
        assertThat(results.getItems().get(1).getId()).isEqualTo(c1.getId());
    }

    @Test
    @Transactional
    void testGetProcessorsFilterByStatus() {
        Bridge b = createBridge();
        Connector c1 = createConnector(b, getConnectorType(), DEFAULT_CONNECTOR_ID + "1", "bar");
        c1.setConditions(createConnectorReadyConditions());
        Connector c2 = createConnector(b, getConnectorType(), DEFAULT_CONNECTOR_ID + "2", "foo");
        c2.setConditions(createConnectorAcceptedConditions());
        bridgeDAO.persist(b);
        getConnectorDAO().persist(c1);
        getConnectorDAO().persist(c2);

        ListResult<Connector> results = getConnectorDAO().findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryResourceInfo(0, 100, filter().by(READY).build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);
        assertThat(results.getItems().get(0).getId()).isEqualTo(c1.getId());
    }

    @Test
    @Transactional
    void testGetProcessorsFilterByNameAndStatus() {
        Bridge b = createBridge();
        Connector c1 = createConnector(b, getConnectorType(), DEFAULT_CONNECTOR_ID + "1", "bar");
        c1.setConditions(createConnectorReadyConditions());
        Connector c2 = createConnector(b, getConnectorType(), DEFAULT_CONNECTOR_ID + "2", "foo");
        c2.setConditions(createConnectorReadyConditions());
        bridgeDAO.persist(b);
        getConnectorDAO().persist(c1);
        getConnectorDAO().persist(c2);

        ListResult<Connector> results = getConnectorDAO().findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryResourceInfo(0, 100, filter().by(c1.getName()).by(READY).build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);
        assertThat(results.getItems().get(0).getId()).isEqualTo(c1.getId());
    }

    @Test
    @Transactional
    public void testGetProcessorsFilterByMoreStatuses() {
        Bridge b = createBridge();
        Connector c1 = createConnector(b, getConnectorType(), DEFAULT_CONNECTOR_ID + "1", "bar");
        c1.setConditions(createConnectorAcceptedConditions());
        Connector c2 = createConnector(b, getConnectorType(), DEFAULT_CONNECTOR_ID + "2", "foo");
        c2.setConditions(createConnectorReadyConditions());
        bridgeDAO.persist(b);
        getConnectorDAO().persist(c1);
        getConnectorDAO().persist(c2);

        ListResult<Connector> results = getConnectorDAO().findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryResourceInfo(0, 100, filter().by(ACCEPTED).by(READY).build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(2L);
        assertThat(results.getTotal()).isEqualTo(2L);

        // Results are sorted descending by default
        assertThat(results.getItems().get(0).getId()).isEqualTo(c2.getId());
        assertThat(results.getItems().get(1).getId()).isEqualTo(c1.getId());
    }

    @Test
    @Transactional
    void testGetProcessorsWithDefaultOrdering() {
        Bridge b = createBridge();
        bridgeDAO.persist(b);

        IntStream.range(0, 5).forEach(i -> {
            String id = String.format("id%s", i);
            String name = String.format("name%s", i);
            Connector c = createConnector(b, getConnectorType(), id, name);
            c.setConditions(createConnectorReadyConditions());
            getConnectorDAO().persist(c);
        });

        ListResult<Connector> results = getConnectorDAO().findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(), new QueryResourceInfo(0, 100));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(5L);
        assertThat(results.getTotal()).isEqualTo(5L);

        // Results are sorted descending by default. The first created is the last to be listed.
        IntStream.range(0, 5).forEach(i -> assertThat(results.getItems().get(4 - i).getId()).isEqualTo(String.format("id%s", i)));
    }
}
