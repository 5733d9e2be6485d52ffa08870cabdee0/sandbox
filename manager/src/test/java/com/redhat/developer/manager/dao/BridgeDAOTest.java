package com.redhat.developer.manager.dao;

import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.manager.TestConstants;
import com.redhat.developer.manager.models.Bridge;
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
        Bridge bridge = buildBridge();
        bridgeDAO.persist(bridge);

        List<Bridge> retrievedBridges = bridgeDAO.findByStatus(BridgeStatus.PROVISIONING);
        Assertions.assertEquals(0, retrievedBridges.size());

        retrievedBridges = bridgeDAO.findByStatus(BridgeStatus.AVAILABLE);
        Assertions.assertEquals(0, retrievedBridges.size());

        retrievedBridges = bridgeDAO.findByStatus(BridgeStatus.REQUESTED);
        Assertions.assertEquals(1, retrievedBridges.size());
    }

    @Test
    public void testFindByNameAndCustomerId() {
        Bridge bridge = buildBridge();
        bridgeDAO.persist(bridge);

        Bridge retrievedBridge = bridgeDAO.findByNameAndCustomerId("not-the-id", TestConstants.DEFAULT_CUSTOMER_ID);
        Assertions.assertNull(retrievedBridge);

        retrievedBridge = bridgeDAO.findByNameAndCustomerId(TestConstants.DEFAULT_BRIDGE_NAME, "not-the-customer-id");
        Assertions.assertNull(retrievedBridge);

        retrievedBridge = bridgeDAO.findByNameAndCustomerId(TestConstants.DEFAULT_BRIDGE_NAME, TestConstants.DEFAULT_CUSTOMER_ID);
        Assertions.assertNotNull(retrievedBridge);
    }

    private Bridge buildBridge() {
        Bridge bridge = new Bridge();
        bridge.setId("myId");
        bridge.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        bridge.setName(TestConstants.DEFAULT_BRIDGE_NAME);
        bridge.setStatus(BridgeStatus.REQUESTED);
        bridge.setSubmittedAt(ZonedDateTime.now());

        return bridge;
    }
}
