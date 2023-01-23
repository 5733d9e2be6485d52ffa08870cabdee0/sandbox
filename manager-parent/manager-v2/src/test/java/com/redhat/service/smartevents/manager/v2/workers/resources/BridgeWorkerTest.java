package com.redhat.service.smartevents.manager.v2.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.manager.core.dns.DnsService;
import com.redhat.service.smartevents.manager.core.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.core.services.RhoasService;
import com.redhat.service.smartevents.manager.core.workers.Work;
import com.redhat.service.smartevents.manager.core.workers.WorkManager;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.ManagedResourceV2;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.Fixtures;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.DEPROVISION;
import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.PREPARING;
import static com.redhat.service.smartevents.manager.v2.workers.resources.WorkerTestUtils.makeWork;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
class BridgeWorkerTest {

    private static final String TEST_RESOURCE_ID = "123";
    private static final String TEST_TOPIC_NAME = "TopicName";
    private static final String TEST_ERROR_HANDLER_TOPIC_NAME = "ErrorHandlerTopicName";

    @InjectMock
    RhoasService rhoasServiceMock;

    @InjectMock
    DnsService dnsServiceMock;

    @InjectMock
    ResourceNamesProvider resourceNamesProviderMock;

    @V2
    @InjectMock
    WorkManager workManagerMock;

    @Inject
    BridgeWorker worker;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void setup() {
        databaseManagerUtils.cleanUp();
        when(resourceNamesProviderMock.getBridgeTopicName(any())).thenReturn(TEST_TOPIC_NAME);
        when(resourceNamesProviderMock.getBridgeErrorTopicName(any())).thenReturn(TEST_ERROR_HANDLER_TOPIC_NAME);
    }

    @Test
    void handleWorkProvisioningWithUnknownResource() {
        Work work = makeWork(TEST_RESOURCE_ID, 0, ZonedDateTime.now(ZoneOffset.UTC));

        assertThatCode(() -> worker.handleWork(work)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @MethodSource("provisionWorkWithKnownResourceParams")
    void testProvisionWorkWithKnownResource(boolean throwRhoasError, boolean throwDnsError, boolean isWorkComplete) {
        Bridge bridge = createAndPersistDefaultAcceptedBridge();

        Work work = makeWork(bridge);

        if (throwRhoasError) {
            when(rhoasServiceMock.createTopicAndGrantAccessFor(any(), any())).thenThrow(new InternalPlatformException("rhoas error"));
        }

        if (throwDnsError) {
            when(dnsServiceMock.createDnsRecord(any())).thenThrow(new InternalPlatformException("dns error"));
        }

        worker.handleWork(work);

        Bridge retrieved = bridgeDAO.findByIdWithConditions(bridge.getId());

        assertThat(StatusUtilities.getManagedResourceStatus(retrieved)).isEqualTo(PREPARING);
        verify(rhoasServiceMock).createTopicAndGrantAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(dnsServiceMock, times(throwRhoasError ? 0 : 1)).createDnsRecord(eq(bridge.getId()));
        verify(workManagerMock, times(isWorkComplete ? 0 : 1)).reschedule(any());
    }

    @Transactional
    protected Bridge createAndPersistDefaultAcceptedBridge() {
        Bridge bridge = Fixtures.createAcceptedBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);
        return bridge;
    }

    private static Stream<Arguments> provisionWorkWithKnownResourceParams() {
        return Stream.of(
                Arguments.of(false, false, true),
                Arguments.of(true, false, false),
                Arguments.of(false, true, false),
                Arguments.of(true, true, false));
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("deletionWorkWithKnownResourceParams")
    void testDeletionWorkWithKnownResource(boolean throwRhoasError, boolean throwDnsError, boolean isWorkComplete) {
        Bridge bridge = createAndPersistDefaultDeprovisionBridge();

        Work work = makeWork(bridge);

        if (throwRhoasError) {
            doThrow(new InternalPlatformException("rhoas error")).when(rhoasServiceMock).deleteTopicAndRevokeAccessFor(eq(TEST_TOPIC_NAME), any());
        }

        if (throwDnsError) {
            doThrow(new InternalPlatformException("dns error")).when(dnsServiceMock).deleteDnsRecord(eq(bridge.getId()));
        }

        Bridge retrieved = worker.handleWork(work);

        assertThat(StatusUtilities.getManagedResourceStatus(retrieved)).isEqualTo(DEPROVISION);
        verify(rhoasServiceMock).deleteTopicAndRevokeAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(dnsServiceMock, times(throwRhoasError ? 0 : 1)).deleteDnsRecord(eq(bridge.getId()));
        verify(workManagerMock, times(isWorkComplete ? 0 : 1)).reschedule(work);
    }

    @Test
    void testBridgeWorkerType() {
        ManagedResourceV2 managedResourceV2 = Fixtures.createBridge();
        Work work = Work.forResource(managedResourceV2);
        BridgeWorker bridgeWorker = new BridgeWorker();

        assertThat(bridgeWorker.accept(work)).isTrue();
    }

    @Transactional
    protected Bridge createAndPersistDefaultDeprovisionBridge() {
        Bridge bridge = Fixtures.createDeprovisioningBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);
        return bridge;
    }

    private static Stream<Arguments> deletionWorkWithKnownResourceParams() {
        return Stream.of(
                Arguments.of(false, false, true),
                Arguments.of(false, true, false),
                Arguments.of(true, false, false),
                Arguments.of(true, true, false));
    }
}
