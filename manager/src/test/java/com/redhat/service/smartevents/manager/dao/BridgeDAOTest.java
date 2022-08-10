package com.redhat.service.smartevents.manager.dao;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.manager.TestConstants;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.Fixtures;

import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.infra.models.QueryFilterInfo.QueryFilterInfoBuilder.filter;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_PAGE;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class BridgeDAOTest {

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
    }

    @Test
    @Transactional
    public void testFindByStatus() {
        Bridge bridge = buildBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        List<Bridge> retrievedBridges = bridgeDAO.findByShardIdToDeployOrDelete(TestConstants.SHARD_ID);
        assertThat(retrievedBridges).isEmpty();

        // Emulate dependencies being completed
        bridge.setStatus(ManagedResourceStatus.PREPARING);
        bridge.setDependencyStatus(READY);
        bridgeDAO.persist(bridge);

        retrievedBridges = bridgeDAO.findByShardIdToDeployOrDelete(TestConstants.SHARD_ID);
        assertThat(retrievedBridges.size()).isEqualTo(1);

        // Emulate dependencies being completed and Operator started provisioning
        bridge.setStatus(ManagedResourceStatus.PROVISIONING);
        bridge.setDependencyStatus(READY);
        bridgeDAO.persist(bridge);

        retrievedBridges = bridgeDAO.findByShardIdToDeployOrDelete(TestConstants.SHARD_ID);
        assertThat(retrievedBridges.size()).isEqualTo(1);

        // Emulate de-provision request
        bridge.setStatus(ManagedResourceStatus.DEPROVISION);
        bridge.setDependencyStatus(READY);
        bridgeDAO.persist(bridge);

        retrievedBridges = bridgeDAO.findByShardIdToDeployOrDelete(TestConstants.SHARD_ID);
        assertThat(retrievedBridges).isEmpty();

        // Emulate dependencies being deleted
        bridge.setDependencyStatus(ManagedResourceStatus.DELETED);
        bridgeDAO.persist(bridge);

        retrievedBridges = bridgeDAO.findByShardIdToDeployOrDelete(TestConstants.SHARD_ID);
        assertThat(retrievedBridges.size()).isEqualTo(1);

        // Emulate dependencies being deleted and Operator started deleting
        bridge.setStatus(ManagedResourceStatus.DELETING);
        bridge.setDependencyStatus(ManagedResourceStatus.DELETED);
        bridgeDAO.persist(bridge);

        retrievedBridges = bridgeDAO.findByShardIdToDeployOrDelete(TestConstants.SHARD_ID);
        assertThat(retrievedBridges.size()).isEqualTo(1);
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
    public void testListByCustomerId() {
        Bridge firstBridge = buildBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(firstBridge);

        Bridge secondBridge = buildBridge("mySecondBridgeId", "mySecondBridgeName");
        bridgeDAO.persist(secondBridge);

        ListResult<Bridge> retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID, new QueryResourceInfo(DEFAULT_PAGE, DEFAULT_PAGE_SIZE));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(2);
        assertThat(retrievedBridges.getTotal()).isEqualTo(2);
        assertThat(retrievedBridges.getPage()).isZero();

        // Newest instances come first
        assertThat(retrievedBridges.getItems().get(1).getId()).isEqualTo(firstBridge.getId());
        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo(secondBridge.getId());
    }

    @Test
    public void testListByCustomerIdFilterByName() {
        Bridge bridge1 = buildBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = buildBridge("mySecondBridgeId", "mySecondBridgeName");
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
        Bridge bridge1 = buildBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = buildBridge("mySecondBridgeId", "mySecondBridgeName");
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
        Bridge bridge1 = buildBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridge1.setStatus(ACCEPTED);
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = buildBridge("mySecondBridgeId", "mySecondBridgeName");
        bridge2.setStatus(READY);
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
        Bridge bridge1 = buildBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridge1.setStatus(ACCEPTED);
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = buildBridge("mySecondBridgeId", "mySecondBridgeName");
        bridge2.setStatus(READY);
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
        Bridge bridge1 = buildBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridge1.setStatus(ACCEPTED);
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = buildBridge("mySecondBridgeId", "mySecondBridgeName");
        bridge2.setStatus(READY);
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
            Bridge bridge = buildBridge(id, id);
            bridge.setStatus(i % 2 == 0 ? READY : ACCEPTED);
            bridgeDAO.persist(bridge);
        }

        ListResult<Bridge> retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID,
                new QueryResourceInfo(0, 2, filter().by(READY).build()));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(2);
        assertThat(retrievedBridges.getTotal()).isEqualTo(5);
        assertThat(retrievedBridges.getPage()).isZero();
        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo("8");
        assertThat(retrievedBridges.getItems().get(1).getId()).isEqualTo("6");

        retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID,
                new QueryResourceInfo(1, 2, filter().by(READY).build()));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(2);
        assertThat(retrievedBridges.getTotal()).isEqualTo(5);
        assertThat(retrievedBridges.getPage()).isEqualTo(1);
        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo("4");
        assertThat(retrievedBridges.getItems().get(1).getId()).isEqualTo("2");

        retrievedBridges = bridgeDAO.findByCustomerId(DEFAULT_CUSTOMER_ID,
                new QueryResourceInfo(2, 2, filter().by(READY).build()));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(1);
        assertThat(retrievedBridges.getTotal()).isEqualTo(5);
        assertThat(retrievedBridges.getPage()).isEqualTo(2);
        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo("0");
    }

    private Bridge buildBridge(String id, String name) {
        Bridge bridge = Fixtures.createBridge();
        bridge.setId(id);
        bridge.setName(name);
        bridge.setStatus(ACCEPTED);
        bridge.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        bridge.setShardId(TestConstants.SHARD_ID);
        return bridge;
    }
}
