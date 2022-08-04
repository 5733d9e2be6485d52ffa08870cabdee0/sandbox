package com.redhat.service.smartevents.manager;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.BadRequestException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.api.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.manager.utils.TestUtils;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.DEPROVISION;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.PROVISIONING;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_BRIDGE_ENDPOINT;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_BRIDGE_TLS_CERTIFICATE;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_BRIDGE_TLS_KEY;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_CLOUD_PROVIDER;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_ORGANISATION_ID;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_PAGE;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_PAGE_SIZE;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_REGION;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_USER_NAME;
import static com.redhat.service.smartevents.manager.TestConstants.SHARD_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class BridgesServiceTest {

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    BridgesService bridgesService;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @InjectMock
    @SuppressWarnings("unused")
    RhoasService rhoasServiceMock;

    @InjectMock
    @SuppressWarnings("unused")
    ProcessorService processorService;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
    }

    @Test
    public void testGetEmptyBridgesToDeploy() {
        List<Bridge> bridges = bridgesService.findByShardIdWithReadyDependencies(SHARD_ID);
        assertThat(bridges.size()).isZero();
    }

    @Test
    public void testGetEmptyBridges() {
        ListResult<Bridge> bridges = bridgesService.getBridges(DEFAULT_CUSTOMER_ID, new QueryResourceInfo(DEFAULT_PAGE, DEFAULT_PAGE_SIZE));
        assertThat(bridges.getPage()).isZero();
        assertThat(bridges.getTotal()).isZero();
        assertThat(bridges.getSize()).isZero();
    }

    @Test
    public void testGetBridges() {
        BridgeRequest request = new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION);
        bridgesService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);

        ListResult<Bridge> bridges = bridgesService.getBridges(DEFAULT_CUSTOMER_ID, new QueryResourceInfo(DEFAULT_PAGE, DEFAULT_PAGE_SIZE));
        assertThat(bridges.getSize()).isEqualTo(1);
        assertThat(bridges.getTotal()).isEqualTo(1);
        assertThat(bridges.getPage()).isZero();

        // filter by customer id not implemented yet
        bridges = bridgesService.getBridges("not-the-id", new QueryResourceInfo(DEFAULT_PAGE, DEFAULT_PAGE_SIZE));
        assertThat(bridges.getSize()).isZero();
        assertThat(bridges.getTotal()).isZero();
        assertThat(bridges.getPage()).isZero();
    }

    @Test
    public void testGetBridge() {
        BridgeRequest request = new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION);
        Bridge bridge = bridgesService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);

        //Wait for Workers to complete
        TestUtils.waitForBridgeToBeReady(bridgesService);

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge).isNotNull();
        assertThat(retrievedBridge.getName()).isEqualTo(bridge.getName());
        assertThat(retrievedBridge.getCustomerId()).isEqualTo(bridge.getCustomerId());
        // Bridges are moved to the PREPARING status by Workers
        assertThat(retrievedBridge.getStatus()).isEqualTo(ManagedResourceStatus.PREPARING);
        assertThat(retrievedBridge.getShardId()).isEqualTo(SHARD_ID);
    }

    @Test
    public void testGetUnexistingBridge() {
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> bridgesService.getBridge("not-the-id", DEFAULT_CUSTOMER_ID));
    }

    @Test
    public void testGetBridgeWithWrongCustomerId() {
        BridgeRequest request = new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION);
        bridgesService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);

        //Wait for Workers to complete
        Bridge bridge = TestUtils.waitForBridgeToBeReady(bridgesService);

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> bridgesService.getBridge(bridge.getId(), "not-the-customerId"));
    }

    @Test
    public void testCreateBridge() {
        BridgeRequest request = new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION);
        bridgesService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);

        //Wait for Workers to complete
        Bridge bridge = TestUtils.waitForBridgeToBeReady(bridgesService);

        assertThat(bridge.getStatus()).isEqualTo(ManagedResourceStatus.PREPARING);
        assertThat(bridge.getEndpoint()).isNotNull();

        ListResult<Bridge> bridges = bridgesService.getBridges(DEFAULT_CUSTOMER_ID, new QueryResourceInfo(DEFAULT_PAGE, DEFAULT_PAGE_SIZE));
        assertThat(bridges.getSize()).isEqualTo(1);
        assertThat(bridges.getItems().get(0).getOrganisationId()).isEqualTo(DEFAULT_ORGANISATION_ID);
    }

    @Test
    void testCreateBridge_whiteSpaceInName() {
        BridgeRequest request = new BridgeRequest("   name   ", DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION);
        Bridge bridge = bridgesService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);
        assertThat(bridge.getName()).isEqualTo("name");
    }

    @Test
    public void testUpdateBridgeStatus() {
        BridgeRequest request = new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION);
        bridgesService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);

        //Wait for Workers to complete
        Bridge bridge = TestUtils.waitForBridgeToBeReady(bridgesService);

        assertThat(bridge.getStatus()).isEqualTo(ManagedResourceStatus.PREPARING);

        // Emulate Shard setting Bridge status to PROVISIONING
        ManagedResourceStatusUpdateDTO updateDTO = new ManagedResourceStatusUpdateDTO(bridge.getId(), bridge.getCustomerId(), PROVISIONING);
        bridgesService.updateBridge(updateDTO);

        // PROVISIONING Bridges are also notified to the Shard Operator.
        // This ensures Bridges are not dropped should the Shard fail after notifying the Managed a Bridge is being provisioned.
        assertThat(bridgesService.findByShardIdWithReadyDependencies(SHARD_ID)).hasSize(1);

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge.getStatus()).isEqualTo(PROVISIONING);
    }

    @Test
    public void testUpdateBridgeStatusReadyPublishedAt() {
        BridgeRequest request = new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION);
        bridgesService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);

        //Wait for Workers to complete
        Bridge bridge = TestUtils.waitForBridgeToBeReady(bridgesService);

        // Emulate Shard setting Bridge status to PROVISIONING
        ManagedResourceStatusUpdateDTO updateDTO = new ManagedResourceStatusUpdateDTO(bridge.getId(), bridge.getCustomerId(), PROVISIONING);
        bridgesService.updateBridge(updateDTO);

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge.getStatus()).isEqualTo(PROVISIONING);
        assertThat(retrievedBridge.getPublishedAt()).isNull();

        // Once ready it should have its published date set
        updateDTO = new ManagedResourceStatusUpdateDTO(bridge.getId(), bridge.getCustomerId(), READY);
        bridgesService.updateBridge(updateDTO);

        Bridge publishedBridge = bridgesService.getBridge(bridge.getId(), DEFAULT_CUSTOMER_ID);
        assertThat(publishedBridge.getStatus()).isEqualTo(READY);
        ZonedDateTime publishedAt = publishedBridge.getPublishedAt();
        assertThat(publishedAt).isNotNull();

        //Check calls to set PublishedAt at idempotent
        bridgesService.updateBridge(updateDTO);

        Bridge publishedBridge2 = bridgesService.getBridge(bridge.getId(), DEFAULT_CUSTOMER_ID);
        assertThat(publishedBridge2.getStatus()).isEqualTo(READY);
        assertThat(publishedBridge2.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    public void getBridge() {
        BridgeRequest request = new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION);
        bridgesService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);

        //Wait for Workers to complete
        Bridge bridge = TestUtils.waitForBridgeToBeReady(bridgesService);

        Bridge found = bridgesService.getBridge(bridge.getId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(bridge.getId());
    }

    @Test
    public void getBridge_bridgeDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> bridgesService.getBridge("foo"));
    }

    @Test
    public void testDeleteBridge() {
        Bridge bridge = createPersistBridge(READY);

        bridgesService.deleteBridge(bridge.getId(), bridge.getCustomerId());

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), bridge.getCustomerId());
        assertThat(retrievedBridge.getStatus()).isEqualTo(DEPROVISION);
        assertThat(retrievedBridge.getDeletionRequestedAt()).isNotNull();
    }

    @Test
    public void testDeleteBridge_whenStatusIsFailed() {
        Bridge bridge = createPersistBridge(ManagedResourceStatus.FAILED);

        bridgesService.deleteBridge(bridge.getId(), bridge.getCustomerId());

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), bridge.getCustomerId());
        assertThat(retrievedBridge.getStatus()).isEqualTo(DEPROVISION);
        assertThat(retrievedBridge.getDeletionRequestedAt()).isNotNull();
    }

    @Test
    public void testDeleteBridge_whenStatusIsNotReady() {
        Bridge bridge = createPersistBridge(PROVISIONING);
        assertThatExceptionOfType(BridgeLifecycleException.class).isThrownBy(() -> bridgesService.deleteBridge(bridge.getId(), bridge.getCustomerId()));
    }

    @Test
    public void testToDTO() {
        Bridge bridge = createPersistBridge(READY);

        BridgeDTO bridgeDTO = bridgesService.toDTO(bridge);

        assertThat(bridgeDTO.getId()).hasSizeGreaterThan(0);
        assertThat(bridgeDTO.getName()).isEqualTo(DEFAULT_BRIDGE_NAME);
        assertThat(bridgeDTO.getEndpoint()).isEqualTo(DEFAULT_BRIDGE_ENDPOINT);
        assertThat(bridgeDTO.getCustomerId()).isEqualTo(DEFAULT_CUSTOMER_ID);
        assertThat(bridgeDTO.getOwner()).isEqualTo(DEFAULT_CUSTOMER_ID);
        assertThat(bridgeDTO.getTlsCertificate()).isEqualTo(DEFAULT_BRIDGE_TLS_CERTIFICATE);
        assertThat(bridgeDTO.getTlsKey()).isEqualTo(DEFAULT_BRIDGE_TLS_KEY);
    }

    @ParameterizedTest
    @MethodSource("updateBridgeParams")
    void testUpdateBridgeWhenBridgeNotExists(BridgeRequestForTests request) {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> bridgesService.updateBridge(DEFAULT_BRIDGE_ID + "-not-exists", DEFAULT_CUSTOMER_ID, request));
    }

    @ParameterizedTest
    @MethodSource("updateBridgeParams")
    void testUpdateBridgeWhenBridgeNotInReadyState(BridgeRequestForTests request) {
        createPersistBridge(TestConstants.DEFAULT_BRIDGE_ID, PROVISIONING);

        assertThatExceptionOfType(BridgeLifecycleException.class)
                .isThrownBy(() -> bridgesService.updateBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @ParameterizedTest
    @MethodSource("updateBridgeParams")
    void testUpdateBridgeWithName(BridgeRequestForTests request) {
        createPersistBridge(TestConstants.DEFAULT_BRIDGE_ID, READY);

        request.setName(request.getName() + "-updated");
        assertThatExceptionOfType(BadRequestException.class)
                .isThrownBy(() -> bridgesService.updateBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @ParameterizedTest
    @MethodSource("updateBridgeParams")
    void testUpdateBridgeWithCloudProvider(BridgeRequestForTests request) {
        createPersistBridge(TestConstants.DEFAULT_BRIDGE_ID, READY);

        request.setCloudProvider(request.getCloudProvider() + "-updated");
        assertThatExceptionOfType(BadRequestException.class)
                .isThrownBy(() -> bridgesService.updateBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @ParameterizedTest
    @MethodSource("updateBridgeParams")
    void testUpdateBridgeWithRegion(BridgeRequestForTests request) {
        createPersistBridge(TestConstants.DEFAULT_BRIDGE_ID, READY);

        request.setRegion(request.getRegion() + "-updated");
        assertThatExceptionOfType(BadRequestException.class)
                .isThrownBy(() -> bridgesService.updateBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @Test
    void testUpdateBridgeErrorHandlerWithNoChange() {
        // Create Bridge without an Error Handler defined
        createPersistBridge(TestConstants.DEFAULT_BRIDGE_ID, READY);

        BridgeRequest request = new BridgeRequestForTests(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION);
        Bridge updatedBridge = bridgesService.updateBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, request);
        BridgeResponse updatedResponse = bridgesService.toResponse(updatedBridge);

        // The Bridge created at the beginning of this test does not have an Error Handler
        // Therefore we do not expect there to have been any changes or Work scheduled.
        assertThat(updatedResponse.getStatus()).isEqualTo(READY);
        assertThat(updatedResponse.getErrorHandler()).isNull();
    }

    @Test
    void testUpdateBridgeErrorHandlerWithNewErrorHandler() {
        // Create Bridge without an Error Handler defined
        createPersistBridge(TestConstants.DEFAULT_BRIDGE_ID, READY);

        BridgeRequest request = new BridgeRequestForTests(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION, TestUtils.createWebhookAction());
        Bridge updatedBridge = bridgesService.updateBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, request);
        BridgeResponse updatedResponse = bridgesService.toResponse(updatedBridge);

        // The Bridge should move into ACCEPTED state to provision the Error Handler
        assertThat(updatedResponse.getStatus()).isEqualTo(ACCEPTED);
        assertThat(updatedResponse.getErrorHandler()).isNotNull();
    }

    protected Bridge createPersistBridge(ManagedResourceStatus status) {
        Bridge b = Fixtures.createBridge();
        b.setStatus(status);
        bridgeDAO.persist(b);
        return b;
    }

    protected Bridge createPersistBridge(String bridgeId, ManagedResourceStatus status) {
        Bridge b = Fixtures.createBridge();
        b.setId(bridgeId);
        b.setStatus(status);
        bridgeDAO.persist(b);
        return b;
    }

    private static Stream<Arguments> updateBridgeParams() {
        Object[] arguments = {
                new BridgeRequestForTests(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION),
                new BridgeRequestForTests(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION, TestUtils.createWebhookAction())
        };
        return Stream.of(arguments).map(Arguments::of);
    }

}
