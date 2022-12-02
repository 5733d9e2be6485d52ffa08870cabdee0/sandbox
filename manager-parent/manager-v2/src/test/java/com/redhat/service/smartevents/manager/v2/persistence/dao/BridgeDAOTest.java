package com.redhat.service.smartevents.manager.v2.persistence.dao;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.Fixtures;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.infra.core.models.queries.QueryFilterInfo.QueryFilterInfoBuilder.filter;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_ORGANISATION_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_PAGE;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_PAGE_SIZE;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridge;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createCondition;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class BridgeDAOTest {

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ConditionDAO conditionDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
    }

    @Test
    @Transactional
    public void testStoreBridge() {
        Bridge bridge = createBridge();
        bridgeDAO.persist(bridge);

        Bridge retrieved = bridgeDAO.findById(bridge.getId());
        assertThat(retrieved.getId()).isEqualTo(bridge.getId());
        assertThat(retrieved.getOperation()).isNotNull();
        assertThat(retrieved.getOperation().getType()).isNotNull();
        assertThat(retrieved.getOperation().getRequestedAt()).isNotNull();
        assertThat(retrieved.getConditions()).isNull();
    }

    @Test
    @Transactional
    public void testStoreBridgeWithCondition() {
        Bridge bridge = createBridge();

        Condition condition = createCondition();

        bridge.setConditions(List.of(condition));
        bridgeDAO.persist(bridge);

        Bridge retrieved = bridgeDAO.findById(bridge.getId());
        assertThat(retrieved.getId()).isEqualTo(bridge.getId());
        assertThat(retrieved.getOperation()).isNotNull();
        assertThat(retrieved.getOperation().getType()).isNotNull();
        assertThat(retrieved.getOperation().getRequestedAt()).isNotNull();
        assertThat(retrieved.getConditions()).hasSize(1);
    }

    @Test
    @Transactional
    public void testStoreBridgeWithConditions() {
        Bridge bridge = createBridge();

        Condition condition1 = createCondition();
        Condition condition2 = createCondition();
        Condition condition3 = createCondition();

        bridge.setConditions(List.of(condition1, condition2, condition3));
        bridgeDAO.persist(bridge);

        Bridge retrieved = bridgeDAO.findById(bridge.getId());
        assertThat(retrieved.getId()).isEqualTo(bridge.getId());
        assertThat(retrieved.getOperation()).isNotNull();
        assertThat(retrieved.getOperation().getType()).isNotNull();
        assertThat(retrieved.getOperation().getRequestedAt()).isNotNull();
        assertThat(retrieved.getConditions()).hasSize(3);
    }

    @Test
    public void testRemovalOfCondition() {
        Bridge bridge = createBridgeWithConditions();
        removeConditionFromBridge(bridge);

        List<Condition> conditions = conditionDAO.findAll().list();

        assertThat(conditions.size()).isEqualTo(2);
    }

    @Transactional
    protected Bridge createBridgeWithConditions() {
        Bridge bridge = createBridge();

        Condition condition1 = createCondition();
        Condition condition2 = createCondition();
        Condition condition3 = createCondition();

        bridge.setConditions(List.of(condition1, condition2, condition3));
        bridgeDAO.persist(bridge);

        return bridge;
    }

    @Transactional
    protected void removeConditionFromBridge(Bridge bridge) {
        Bridge retrieved = bridgeDAO.findById(bridge.getId());
        assertThat(retrieved.getConditions()).hasSize(3);

        Condition condition3 = retrieved.getConditions().get(2);
        retrieved.getConditions().remove(condition3);
        bridgeDAO.persist(retrieved);
    }

    @Test
    public void testFindByNameAndCustomerId() {
        Bridge bridge = buildBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        Bridge retrievedBridge = bridgeDAO.findByNameAndCustomerId("not-the-id", DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge).isNull();

        retrievedBridge = bridgeDAO.findByNameAndCustomerId(DEFAULT_BRIDGE_NAME, "not-the-customer-id");
        assertThat(retrievedBridge).isNull();

        retrievedBridge = bridgeDAO.findByNameAndCustomerId(DEFAULT_BRIDGE_NAME, DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge).isNotNull();
    }

    @Test
    public void testCountByOrganisationId() {
        for (int i = 0; i < 10; i++) {
            String id = String.valueOf(i);
            Bridge bridge = buildBridge(id, id);
            bridgeDAO.persist(bridge);
        }

        long countBridges = bridgeDAO.countByOrganisationId(DEFAULT_ORGANISATION_ID);
        assertThat(countBridges).isEqualTo(10);

        countBridges = bridgeDAO.countByOrganisationId("random");
        assertThat(countBridges).isEqualTo(0);
    }

    @Test
    public void testListByCustomerId() {
        Bridge bridge1 = Fixtures.createAcceptedBridge(DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = Fixtures.createAcceptedBridge("mySecondBridgeName");
        bridgeDAO.persist(bridge2);

        ListResult<Bridge> retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID, new QueryResourceInfo(DEFAULT_PAGE, DEFAULT_PAGE_SIZE));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(2);
        assertThat(retrievedBridges.getTotal()).isEqualTo(2);
        assertThat(retrievedBridges.getPage()).isZero();

        // Newest instances come first
        assertThat(retrievedBridges.getItems().get(1).getId()).isEqualTo(bridge1.getId());
        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo(bridge2.getId());
    }

    @Test
    public void testListByCustomerIdFilterByName() {
        Bridge bridge1 = Fixtures.createAcceptedBridge(DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = Fixtures.createAcceptedBridge("mySecondBridgeName");
        bridgeDAO.persist(bridge2);

        ListResult<Bridge> retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID,
                new QueryResourceInfo(DEFAULT_PAGE, DEFAULT_PAGE_SIZE, filter().by(DEFAULT_BRIDGE_NAME).build()));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(1);
        assertThat(retrievedBridges.getTotal()).isEqualTo(1);
        assertThat(retrievedBridges.getPage()).isZero();

        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo(bridge1.getId());
    }

    @Test
    public void testListByCustomerIdFilterByNameWildcard() {
        Bridge bridge1 = Fixtures.createAcceptedBridge(DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = Fixtures.createAcceptedBridge("mySecondBridgeName");
        bridgeDAO.persist(bridge2);

        ListResult<Bridge> retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID,
                new QueryResourceInfo(DEFAULT_PAGE, DEFAULT_PAGE_SIZE, filter().by(DEFAULT_BRIDGE_NAME.substring(0, 5)).build()));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(1);
        assertThat(retrievedBridges.getTotal()).isEqualTo(1);
        assertThat(retrievedBridges.getPage()).isZero();

        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo(bridge1.getId());
    }

    @Test
    public void testListByCustomerIdFilterByStatus() {
        Bridge bridge1 = Fixtures.createAcceptedBridge(DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = Fixtures.createReadyBridge("mySecondBridgeName");
        bridgeDAO.persist(bridge2);

        ListResult<Bridge> retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID,
                new QueryResourceInfo(DEFAULT_PAGE, DEFAULT_PAGE_SIZE, filter().by(READY).build()));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(1);
        assertThat(retrievedBridges.getTotal()).isEqualTo(1);
        assertThat(retrievedBridges.getPage()).isZero();

        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo(bridge2.getId());
    }

    @Test
    public void testListByCustomerIdFilterByNameAndStatus() {
        Bridge bridge1 = Fixtures.createAcceptedBridge(DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = Fixtures.createReadyBridge("mySecondBridgeName");
        bridgeDAO.persist(bridge2);

        ListResult<Bridge> retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID,
                new QueryResourceInfo(DEFAULT_PAGE, DEFAULT_PAGE_SIZE, filter().by(DEFAULT_BRIDGE_NAME).by(ACCEPTED).build()));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(1);
        assertThat(retrievedBridges.getTotal()).isEqualTo(1);
        assertThat(retrievedBridges.getPage()).isZero();

        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo(bridge1.getId());
    }

    @Test
    public void testListByCustomerIdFilterByMoreStatuses() {
        Bridge bridge1 = Fixtures.createAcceptedBridge(DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = Fixtures.createReadyBridge("mySecondBridgeName");
        bridgeDAO.persist(bridge2);

        ListResult<Bridge> retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID,
                new QueryResourceInfo(DEFAULT_PAGE, DEFAULT_PAGE_SIZE, filter().by(ACCEPTED).by(READY).build()));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(2);
        assertThat(retrievedBridges.getTotal()).isEqualTo(2);
        assertThat(retrievedBridges.getPage()).isZero();

        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo(bridge2.getId());
        assertThat(retrievedBridges.getItems().get(1).getId()).isEqualTo(bridge1.getId());
    }

    @Test
    public void testListByCustomerIdPagination() {
        for (int i = 0; i < 10; i++) {
            String id = String.valueOf(i);
            Bridge bridge = buildBridge(id, id);
            bridgeDAO.persist(bridge);
        }

        ListResult<Bridge> retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID, new QueryResourceInfo(0, 2));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(2);
        assertThat(retrievedBridges.getTotal()).isEqualTo(10);
        assertThat(retrievedBridges.getPage()).isZero();
        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo("9");
        assertThat(retrievedBridges.getItems().get(1).getId()).isEqualTo("8");

        retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID, new QueryResourceInfo(1, 2));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(2);
        assertThat(retrievedBridges.getTotal()).isEqualTo(10);
        assertThat(retrievedBridges.getPage()).isEqualTo(1);
        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo("7");
        assertThat(retrievedBridges.getItems().get(1).getId()).isEqualTo("6");

        retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID, new QueryResourceInfo(4, 2));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(2);
        assertThat(retrievedBridges.getTotal()).isEqualTo(10);
        assertThat(retrievedBridges.getPage()).isEqualTo(4);
        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo("1");
        assertThat(retrievedBridges.getItems().get(1).getId()).isEqualTo("0");

        retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID, new QueryResourceInfo(5, 2));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isZero();
        assertThat(retrievedBridges.getTotal()).isEqualTo(10);
        assertThat(retrievedBridges.getPage()).isEqualTo(5);
    }

    @Test
    public void testListByCustomerIdPaginationFilterByStatus() {
        for (int i = 0; i < 10; i++) {
            String id = String.valueOf(i);
            Bridge bridge = i % 2 == 0 ? Fixtures.createReadyBridge(id) : Fixtures.createAcceptedBridge(id);
            bridgeDAO.persist(bridge);
        }

        ListResult<Bridge> retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID,
                new QueryResourceInfo(0, 2, filter().by(READY).build()));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(2);
        assertThat(retrievedBridges.getTotal()).isEqualTo(5);
        assertThat(retrievedBridges.getPage()).isZero();
        assertThat(retrievedBridges.getItems().get(0).getName()).isEqualTo("8");
        assertThat(retrievedBridges.getItems().get(1).getName()).isEqualTo("6");

        retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID,
                new QueryResourceInfo(1, 2, filter().by(READY).build()));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(2);
        assertThat(retrievedBridges.getTotal()).isEqualTo(5);
        assertThat(retrievedBridges.getPage()).isEqualTo(1);
        assertThat(retrievedBridges.getItems().get(0).getName()).isEqualTo("4");
        assertThat(retrievedBridges.getItems().get(1).getName()).isEqualTo("2");

        retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID,
                new QueryResourceInfo(2, 2, filter().by(READY).build()));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(1);
        assertThat(retrievedBridges.getTotal()).isEqualTo(5);
        assertThat(retrievedBridges.getPage()).isEqualTo(2);
        assertThat(retrievedBridges.getItems().get(0).getName()).isEqualTo("0");
    }

    private Bridge buildBridge(String id, String name) {
        Bridge bridge = createBridge();
        bridge.setId(id);
        bridge.setName(name);
        return bridge;
    }
}
