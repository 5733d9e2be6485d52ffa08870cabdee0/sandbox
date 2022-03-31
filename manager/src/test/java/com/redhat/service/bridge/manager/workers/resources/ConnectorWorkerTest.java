package com.redhat.service.bridge.manager.workers.resources;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorState;
import com.openshift.cloud.api.connector.models.ConnectorStatusStatus;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.connectors.ConnectorsApiClient;
import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.models.Work;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.manager.utils.Fixtures;
import com.redhat.service.bridge.manager.workers.WorkManager;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;
import com.redhat.service.bridge.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
class ConnectorWorkerTest {

    private static final String TEST_PROCESSOR_NAME = "TestProcessor";
    private static final String TEST_CONNECTOR_NAME = "TestConnector";
    private static final String TEST_CONNECTOR_EXTERNAL_ID = "connectorExternalId";
    private static final String TEST_RESOURCE_ID = "123";
    private static final String TEST_TOPIC_NAME = "TopicName";

    @InjectMock
    RhoasService rhoasService;

    @InjectMock
    ConnectorsApiClient connectorsApi;

    @Inject
    WorkManager workManager;

    @Inject
    ConnectorWorker worker;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    ConnectorsDAO connectorsDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void setup() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
    }

    @Test
    void handleWorkProvisioningWithUnknownResource() {
        Work work = new Work();
        work.setManagedResourceId(TEST_RESOURCE_ID);

        assertThatCode(() -> worker.handleWork(work)).isInstanceOf(IllegalStateException.class);
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("provideArgsForCreateTest")
    void handleWorkProvisioningWithKnownResourceMultiplePasses(
            ManagedResourceStatus resourceStatus,
            ConnectorState connectorState,
            ManagedResourceStatus expectedResourceStatus) {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, TEST_PROCESSOR_NAME, ManagedResourceStatus.READY);
        ConnectorEntity connectorEntity = Fixtures.createConnector(processor, TEST_CONNECTOR_NAME, resourceStatus, TEST_TOPIC_NAME);
        connectorEntity.setPublishedAt(null);//The publishedAt date is set by the Worker so reset that set by the Fixture
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);
        connectorsDAO.persist(connectorEntity);

        Work work = workManager.schedule(connectorEntity);

        Connector connector = new Connector();
        connector.setId(TEST_CONNECTOR_EXTERNAL_ID);
        connector.setStatus(new ConnectorStatusStatus().state(connectorState));

        when(connectorsApi.getConnector(TEST_CONNECTOR_EXTERNAL_ID)).thenReturn(null, connector);
        when(connectorsApi.createConnector(connectorEntity)).thenReturn(connector);

        ConnectorEntity refreshed = worker.handleWork(work);

        verify(rhoasService).createTopicAndGrantAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);
        verify(connectorsApi).createConnector(connectorEntity);
        assertThat(refreshed.getStatus()).isEqualTo(ManagedResourceStatus.PROVISIONING);
        assertThat(refreshed.getDependencyStatus()).isEqualTo(ManagedResourceStatus.PROVISIONING);

        // This emulates a subsequent invocation by WorkManager
        refreshed = worker.handleWork(work);

        verify(rhoasService, times(2)).createTopicAndGrantAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);
        verify(connectorsApi, atMostOnce()).createConnector(connectorEntity);

        assertThat(refreshed.getStatus()).isEqualTo(expectedResourceStatus);
        assertThat(refreshed.getDependencyStatus()).isEqualTo(expectedResourceStatus);
        if (expectedResourceStatus == ManagedResourceStatus.READY) {
            assertThat(refreshed.getPublishedAt()).isNotNull();
        } else {
            assertThat(refreshed.getPublishedAt()).isNull();
        }
        assertThat(refreshed.getModifiedAt()).isNotNull();
        assertThat(workManager.exists(work)).isTrue();
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("provideArgsForDeleteTest")
    void handleWorkDeletingWithKnownResourceMultiplePasses(
            ManagedResourceStatus resourceStatus,
            ConnectorState connectorState) {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, TEST_PROCESSOR_NAME, ManagedResourceStatus.READY);
        ConnectorEntity connectorEntity = Fixtures.createConnector(processor, TEST_CONNECTOR_NAME, resourceStatus, TEST_TOPIC_NAME);
        connectorEntity.setPublishedAt(null);//The publishedAt date is set by the Worker so reset that set by the Fixture
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);
        connectorsDAO.persist(connectorEntity);

        Work work = workManager.schedule(connectorEntity);

        Connector connector = new Connector();
        connector.setStatus(new ConnectorStatusStatus().state(connectorState));

        // Managed Connector will initially be available before it is deleted
        when(connectorsApi.getConnector(connectorEntity.getConnectorExternalId())).thenReturn(connector, null);

        ConnectorEntity refreshed = worker.handleWork(work);

        if (connectorState != ConnectorState.DELETED) {
            assertThat(refreshed.getStatus()).isEqualTo(ManagedResourceStatus.DELETING);
            assertThat(refreshed.getDependencyStatus()).isEqualTo(ManagedResourceStatus.DELETING);
            verify(rhoasService, never()).deleteTopicAndRevokeAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);
            verify(connectorsApi).deleteConnector(connectorEntity.getConnectorExternalId());

            // This emulates a subsequent invocation by WorkManager
            refreshed = worker.handleWork(work);
        }

        verify(rhoasService).deleteTopicAndRevokeAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);
        assertThat(connectorsDAO.findById(connectorEntity.getId())).isNull();

        assertThat(refreshed.getStatus()).isEqualTo(ManagedResourceStatus.DELETED);
        assertThat(refreshed.getDependencyStatus()).isEqualTo(ManagedResourceStatus.DELETED);
        assertThat(refreshed.getModifiedAt()).isNotNull();
        assertThat(workManager.exists(work)).isTrue();
    }

    private static Stream<Arguments> provideArgsForCreateTest() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.ACCEPTED, ConnectorState.READY, ManagedResourceStatus.READY),
                Arguments.of(ManagedResourceStatus.ACCEPTED, ConnectorState.FAILED, ManagedResourceStatus.FAILED),
                Arguments.of(ManagedResourceStatus.PROVISIONING, ConnectorState.READY, ManagedResourceStatus.READY),
                Arguments.of(ManagedResourceStatus.PROVISIONING, ConnectorState.FAILED, ManagedResourceStatus.FAILED));
    }

    private static Stream<Arguments> provideArgsForDeleteTest() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.DEPROVISION, ConnectorState.READY),
                Arguments.of(ManagedResourceStatus.DEPROVISION, ConnectorState.FAILED),
                Arguments.of(ManagedResourceStatus.DEPROVISION, ConnectorState.DELETED),
                Arguments.of(ManagedResourceStatus.DELETING, ConnectorState.READY),
                Arguments.of(ManagedResourceStatus.DELETING, ConnectorState.FAILED),
                Arguments.of(ManagedResourceStatus.DELETING, ConnectorState.DELETED));
    }
}
