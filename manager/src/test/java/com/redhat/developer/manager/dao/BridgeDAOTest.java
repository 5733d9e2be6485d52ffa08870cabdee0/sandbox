package com.redhat.developer.manager.dao;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.manager.TestConstants;
import com.redhat.developer.manager.models.Bridge;
import com.redhat.developer.manager.models.ListResult;
import com.redhat.developer.manager.utils.DatabaseManagerUtils;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BridgeDAOTest {

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanDatabase();
    }

    @Test
    public void testFindByStatus() {
        Bridge bridge = buildBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        List<Bridge> retrievedBridges = bridgeDAO.findByStatuses(Collections.singletonList(BridgeStatus.PROVISIONING));
        Assertions.assertEquals(0, retrievedBridges.size());

        retrievedBridges = bridgeDAO.findByStatuses(Collections.singletonList(BridgeStatus.AVAILABLE));
        Assertions.assertEquals(0, retrievedBridges.size());

        retrievedBridges = bridgeDAO.findByStatuses(Collections.singletonList(BridgeStatus.REQUESTED));
        Assertions.assertEquals(1, retrievedBridges.size());
    }

    @Test
    public void testFindByNameAndCustomerId() {
        Bridge bridge = buildBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        Bridge retrievedBridge = bridgeDAO.findByNameAndCustomerId("not-the-id", TestConstants.DEFAULT_CUSTOMER_ID);
        Assertions.assertNull(retrievedBridge);

        retrievedBridge = bridgeDAO.findByNameAndCustomerId(TestConstants.DEFAULT_BRIDGE_NAME, "not-the-customer-id");
        Assertions.assertNull(retrievedBridge);

        retrievedBridge = bridgeDAO.findByNameAndCustomerId(TestConstants.DEFAULT_BRIDGE_NAME, TestConstants.DEFAULT_CUSTOMER_ID);
        Assertions.assertNotNull(retrievedBridge);
    }

    @Test
    public void testListByCustomerId() {
        Bridge firstBridge = buildBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(firstBridge);

        Bridge secondBridge = buildBridge("mySecondBridgeId", "mySecondBridgeName");
        bridgeDAO.persist(secondBridge);

        ListResult<Bridge> retrievedBridges = bridgeDAO.findByCustomerId(TestConstants.DEFAULT_CUSTOMER_ID, TestConstants.DEFAULT_PAGE, TestConstants.DEFAULT_PAGE_SIZE);
        Assertions.assertNotNull(retrievedBridges);
        Assertions.assertEquals(2, retrievedBridges.getSize());
        Assertions.assertEquals(2, retrievedBridges.getTotal());
        Assertions.assertEquals(0, retrievedBridges.getPage());

        // Newest istances come first
        Assertions.assertEquals(firstBridge.getId(), retrievedBridges.getItems().get(1).getId());
        Assertions.assertEquals(secondBridge.getId(), retrievedBridges.getItems().get(0).getId());
    }

    @Test
    public void testListByCustomerIdPagination() {
        for (int i = 0; i < 10; i++) {
            String id = String.valueOf(i);
            Bridge bridge = buildBridge(id, id);
            bridgeDAO.persist(bridge);
        }

        ListResult<Bridge> retrievedBridges = bridgeDAO.findByCustomerId(TestConstants.DEFAULT_CUSTOMER_ID, 0, 2);
        Assertions.assertNotNull(retrievedBridges);
        Assertions.assertEquals(2, retrievedBridges.getSize());
        Assertions.assertEquals(10, retrievedBridges.getTotal());
        Assertions.assertEquals(0, retrievedBridges.getPage());
        Assertions.assertEquals("9", retrievedBridges.getItems().get(0).getId());
        Assertions.assertEquals("8", retrievedBridges.getItems().get(1).getId());

        retrievedBridges = bridgeDAO.findByCustomerId(TestConstants.DEFAULT_CUSTOMER_ID, 1, 2);
        Assertions.assertNotNull(retrievedBridges);
        Assertions.assertEquals(2, retrievedBridges.getSize());
        Assertions.assertEquals(10, retrievedBridges.getTotal());
        Assertions.assertEquals(1, retrievedBridges.getPage());
        Assertions.assertEquals("7", retrievedBridges.getItems().get(0).getId());
        Assertions.assertEquals("6", retrievedBridges.getItems().get(1).getId());

        retrievedBridges = bridgeDAO.findByCustomerId(TestConstants.DEFAULT_CUSTOMER_ID, 4, 2);
        Assertions.assertNotNull(retrievedBridges);
        Assertions.assertEquals(2, retrievedBridges.getSize());
        Assertions.assertEquals(10, retrievedBridges.getTotal());
        Assertions.assertEquals(4, retrievedBridges.getPage());
        Assertions.assertEquals("1", retrievedBridges.getItems().get(0).getId());
        Assertions.assertEquals("0", retrievedBridges.getItems().get(1).getId());

        retrievedBridges = bridgeDAO.findByCustomerId(TestConstants.DEFAULT_CUSTOMER_ID, 5, 2);
        Assertions.assertNotNull(retrievedBridges);
        Assertions.assertEquals(0, retrievedBridges.getSize());
        Assertions.assertEquals(10, retrievedBridges.getTotal());
        Assertions.assertEquals(5, retrievedBridges.getPage());
    }

    private Bridge buildBridge(String id, String name) {
        Bridge bridge = new Bridge();
        bridge.setId(id);
        bridge.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        bridge.setName(name);
        bridge.setStatus(BridgeStatus.REQUESTED);
        bridge.setSubmittedAt(ZonedDateTime.now());

        return bridge;
    }
}
