package com.redhat.service.bridge.manager.dao;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;

import io.quarkus.test.junit.QuarkusTest;

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
    public void testFindByStatus() {
        Bridge bridge = buildBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        List<Bridge> retrievedBridges = bridgeDAO.findByStatusesAndShardId(Collections.singletonList(BridgeStatus.PROVISIONING), TestConstants.SHARD_ID);
        assertThat(retrievedBridges.size()).isZero();

        retrievedBridges = bridgeDAO.findByStatusesAndShardId(Collections.singletonList(BridgeStatus.READY), TestConstants.SHARD_ID);
        assertThat(retrievedBridges.size()).isZero();

        retrievedBridges = bridgeDAO.findByStatusesAndShardId(Collections.singletonList(BridgeStatus.ACCEPTED), TestConstants.SHARD_ID);
        assertThat(retrievedBridges.size()).isEqualTo(1);
    }

    @Test
    public void testFindByNameAndCustomerId() {
        Bridge bridge = buildBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        Bridge retrievedBridge = bridgeDAO.findByNameAndCustomerId("not-the-id", TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge).isNull();

        retrievedBridge = bridgeDAO.findByNameAndCustomerId(TestConstants.DEFAULT_BRIDGE_NAME, "not-the-customer-id");
        assertThat(retrievedBridge).isNull();

        retrievedBridge = bridgeDAO.findByNameAndCustomerId(TestConstants.DEFAULT_BRIDGE_NAME, TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge).isNotNull();
    }

    @Test
    public void testListByCustomerId() {
        Bridge firstBridge = buildBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(firstBridge);

        Bridge secondBridge = buildBridge("mySecondBridgeId", "mySecondBridgeName");
        bridgeDAO.persist(secondBridge);

        ListResult<Bridge> retrievedBridges = bridgeDAO.findByCustomerId(TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(TestConstants.DEFAULT_PAGE, TestConstants.DEFAULT_PAGE_SIZE));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(2);
        assertThat(retrievedBridges.getTotal()).isEqualTo(2);
        assertThat(retrievedBridges.getPage()).isZero();

        // Newest istances come first
        assertThat(retrievedBridges.getItems().get(1).getId()).isEqualTo(firstBridge.getId());
        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo(secondBridge.getId());
    }

    @Test
    public void testListByCustomerIdPagination() {
        for (int i = 0; i < 10; i++) {
            String id = String.valueOf(i);
            Bridge bridge = buildBridge(id, id);
            bridgeDAO.persist(bridge);
        }

        ListResult<Bridge> retrievedBridges = bridgeDAO.findByCustomerId(TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(0, 2));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(2);
        assertThat(retrievedBridges.getTotal()).isEqualTo(10);
        assertThat(retrievedBridges.getPage()).isZero();
        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo("9");
        assertThat(retrievedBridges.getItems().get(1).getId()).isEqualTo("8");

        retrievedBridges = bridgeDAO.findByCustomerId(TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(1, 2));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(2);
        assertThat(retrievedBridges.getTotal()).isEqualTo(10);
        assertThat(retrievedBridges.getPage()).isEqualTo(1);
        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo("7");
        assertThat(retrievedBridges.getItems().get(1).getId()).isEqualTo("6");

        retrievedBridges = bridgeDAO.findByCustomerId(TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(4, 2));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isEqualTo(2);
        assertThat(retrievedBridges.getTotal()).isEqualTo(10);
        assertThat(retrievedBridges.getPage()).isEqualTo(4);
        assertThat(retrievedBridges.getItems().get(0).getId()).isEqualTo("1");
        assertThat(retrievedBridges.getItems().get(1).getId()).isEqualTo("0");

        retrievedBridges = bridgeDAO.findByCustomerId(TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(5, 2));
        assertThat(retrievedBridges).isNotNull();
        assertThat(retrievedBridges.getSize()).isZero();
        assertThat(retrievedBridges.getTotal()).isEqualTo(10);
        assertThat(retrievedBridges.getPage()).isEqualTo(5);
    }

    private Bridge buildBridge(String id, String name) {
        Bridge bridge = new Bridge();
        bridge.setId(id);
        bridge.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        bridge.setName(name);
        bridge.setStatus(BridgeStatus.ACCEPTED);
        bridge.setSubmittedAt(ZonedDateTime.now());
        bridge.setShardId(TestConstants.SHARD_ID);

        return bridge;
    }

    @Test
    public void testFindByIdOrNameAndCustomerId() {
        Bridge bridge = buildBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        Bridge retrievedBridge = bridgeDAO.findByIdOrNameAndCustomerId("not-the-id", TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge).isNull();

        retrievedBridge = bridgeDAO.findByIdOrNameAndCustomerId(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge).isNotNull();

        retrievedBridge = bridgeDAO.findByNameAndCustomerId(TestConstants.DEFAULT_BRIDGE_NAME, TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge).isNotNull();
    }
}
