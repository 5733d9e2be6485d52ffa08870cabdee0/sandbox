package com.redhat.service.bridge.manager;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.test.resource.PostgresResource;
import com.redhat.service.bridge.test.rhoas.MasSSOMockServerConfigurator;
import com.redhat.service.bridge.test.rhoas.RedHatSSOMockServerConfiguration;
import com.redhat.service.bridge.test.rhoas.RhoasMockServerResource;
import com.redhat.service.bridge.test.wiremock.InjectWireMock;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
@QuarkusTestResource(value = RhoasMockServerResource.class, restrictToAnnotatedClass = true)
public class BridgesServiceTest {

    @Inject
    BridgesService bridgesService;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @InjectWireMock
    WireMockServer wireMockServer;
    @Inject
    RedHatSSOMockServerConfiguration redHatSSOConfigurator;
    @Inject
    MasSSOMockServerConfigurator masSSOConfigurator;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanDatabase();

        wireMockServer.resetAll();
        redHatSSOConfigurator.configure(wireMockServer);
        masSSOConfigurator.configure(wireMockServer);
    }

    @Test
    public void testGetEmptyBridgesToDeploy() {
        List<Bridge> bridges = bridgesService.getBridgesByStatuses(Collections.singletonList(BridgeStatus.REQUESTED));
        assertThat(bridges.size()).isZero();
    }

    @Test
    public void testGetEmptyBridges() {
        ListResult<Bridge> bridges = bridgesService.getBridges(TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(TestConstants.DEFAULT_PAGE, TestConstants.DEFAULT_PAGE_SIZE));
        assertThat(bridges.getPage()).isZero();
        assertThat(bridges.getTotal()).isZero();
        assertThat(bridges.getSize()).isZero();
    }

    @Test
    public void testGetBridges() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        ListResult<Bridge> bridges = bridgesService.getBridges(TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(TestConstants.DEFAULT_PAGE, TestConstants.DEFAULT_PAGE_SIZE));
        assertThat(bridges.getSize()).isEqualTo(1);
        assertThat(bridges.getTotal()).isEqualTo(1);
        assertThat(bridges.getPage()).isZero();

        // filter by customer id not implemented yet
        bridges = bridgesService.getBridges("not-the-id", new QueryInfo(TestConstants.DEFAULT_PAGE, TestConstants.DEFAULT_PAGE_SIZE));
        assertThat(bridges.getSize()).isZero();
        assertThat(bridges.getTotal()).isZero();
        assertThat(bridges.getPage()).isZero();
    }

    @Test
    public void testGetBridge() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        Bridge bridge = bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge).isNotNull();
        assertThat(retrievedBridge.getName()).isEqualTo(bridge.getName());
        assertThat(retrievedBridge.getCustomerId()).isEqualTo(bridge.getCustomerId());
        assertThat(retrievedBridge.getStatus()).isEqualTo(bridge.getStatus());
    }

    @Test
    public void testGetUnexistingBridge() {
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> bridgesService.getBridge("not-the-id", TestConstants.DEFAULT_CUSTOMER_ID));
    }

    @Test
    public void testGetBridgeWithWrongCustomerId() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        Bridge bridge = bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> bridgesService.getBridge(bridge.getId(), "not-the-customerId"));
    }

    @Test
    public void testCreateBridge() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        List<Bridge> bridgesToDeploy = bridgesService.getBridgesByStatuses(Collections.singletonList(BridgeStatus.REQUESTED));
        assertThat(bridgesToDeploy.size()).isEqualTo(1);
        assertThat(bridgesToDeploy.get(0).getStatus()).isEqualTo(BridgeStatus.REQUESTED);
        assertThat(bridgesToDeploy.get(0).getEndpoint()).isNull();

        ListResult<Bridge> bridges = bridgesService.getBridges(TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(TestConstants.DEFAULT_PAGE, TestConstants.DEFAULT_PAGE_SIZE));
        assertThat(bridges.getSize()).isEqualTo(1);
    }

    @Test
    public void testUpdateBridgeStatus() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        Bridge bridge = bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        List<Bridge> bridges = bridgesService.getBridgesByStatuses(Collections.singletonList(BridgeStatus.REQUESTED));
        assertThat(bridges.size()).isEqualTo(1);
        assertThat(bridges.get(0).getStatus()).isEqualTo(BridgeStatus.REQUESTED);

        bridge.setStatus(BridgeStatus.PROVISIONING);
        bridgesService.updateBridge(bridgesService.toDTO(bridge));

        bridges = bridgesService.getBridgesByStatuses(Collections.singletonList(BridgeStatus.REQUESTED));
        assertThat(bridges.size()).isZero();

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge.getStatus()).isEqualTo(BridgeStatus.PROVISIONING);
    }

    @Test
    public void getBridge() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        Bridge bridge = bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        Bridge found = bridgesService.getBridge(bridge.getId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(bridge.getId());
    }

    @Test
    public void getBridge_bridgeDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> bridgesService.getBridge("foo"));
    }
}
