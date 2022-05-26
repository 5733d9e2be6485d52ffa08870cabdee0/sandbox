package com.redhat.service.smartevents.manager.workers.resources;

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
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.manager.RhoasService;
import com.redhat.service.smartevents.manager.connectors.ConnectorsApiClient;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.dao.ConnectorsDAO;
import com.redhat.service.smartevents.manager.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.models.Work;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.manager.workers.WorkManager;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;
import com.redhat.service.smartevents.test.resource.PostgresResource;

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

    private static final String TEST_CONNECTOR_EXTERNAL_ID = "connectorExternalId";
    private static final String TEST_RESOURCE_ID = "123";

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
            boolean useSourceConnectorEntity,
            RhoasTopicAccessType expectedTopicAccessType,
            ManagedResourceStatus expectedResourceStatus) {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.READY);
        ConnectorEntity connectorEntity = useSourceConnectorEntity
                ? Fixtures.createSourceConnector(processor, resourceStatus)
                : Fixtures.createSinkConnector(processor, resourceStatus);
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

        verify(rhoasService).createTopicAndGrantAccessFor(connectorEntity.getTopicName(), expectedTopicAccessType);
        verify(connectorsApi).createConnector(connectorEntity);
        assertThat(refreshed.getStatus()).isEqualTo(ManagedResourceStatus.PREPARING);
        assertThat(refreshed.getDependencyStatus()).isEqualTo(ManagedResourceStatus.PROVISIONING);

        // This emulates a subsequent invocation by WorkManager
        refreshed = worker.handleWork(work);

        verify(rhoasService, times(2)).createTopicAndGrantAccessFor(connectorEntity.getTopicName(), expectedTopicAccessType);
        verify(connectorsApi, atMostOnce()).createConnector(connectorEntity);

        assertThat(refreshed.getStatus()).isEqualTo(expectedResourceStatus);
        assertThat(refreshed.getDependencyStatus()).isEqualTo(expectedResourceStatus);
        if (expectedResourceStatus == ManagedResourceStatus.READY) {
            assertThat(refreshed.getPublishedAt()).isNotNull();
        } else {
            assertThat(refreshed.getPublishedAt()).isNull();
        }
        assertThat(workManager.exists(work)).isTrue();
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("provideArgsForDeleteTest")
    void handleWorkDeletingWithKnownResourceMultiplePasses(
            ManagedResourceStatus resourceStatus,
            ConnectorState connectorState,
            boolean useSourceConnectorEntity,
            RhoasTopicAccessType expectedTopicAccessType) {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.READY);
        ConnectorEntity connectorEntity = useSourceConnectorEntity
                ? Fixtures.createSourceConnector(processor, resourceStatus)
                : Fixtures.createSinkConnector(processor, resourceStatus);
        connectorEntity.setPublishedAt(null);//The publishedAt date is set by the Worker so reset that set by the Fixture
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);
        connectorsDAO.persist(connectorEntity);

        Work work = workManager.schedule(connectorEntity);

        Connector connector = new Connector();
        connector.setStatus(new ConnectorStatusStatus().state(connectorState));

        // Managed Connector will initially be available before it is deleted
        when(connectorsApi.getConnector(connectorEntity.getConnectorExternalId())).thenReturn(connector, (Connector) null);

        ConnectorEntity refreshed = worker.handleWork(work);

        if (connectorState != ConnectorState.DELETED) {
            assertThat(refreshed.getStatus()).isEqualTo(ManagedResourceStatus.DELETING);
            assertThat(refreshed.getDependencyStatus()).isEqualTo(ManagedResourceStatus.DELETING);
            verify(rhoasService, never()).deleteTopicAndRevokeAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);
            verify(connectorsApi).deleteConnector(connectorEntity.getConnectorExternalId());

            // This emulates a subsequent invocation by WorkManager
            refreshed = worker.handleWork(work);
        }

        verify(rhoasService).deleteTopicAndRevokeAccessFor(connectorEntity.getTopicName(), expectedTopicAccessType);
        assertThat(connectorsDAO.findById(connectorEntity.getId())).isNull();

        assertThat(refreshed.getStatus()).isEqualTo(ManagedResourceStatus.DELETED);
        assertThat(refreshed.getDependencyStatus()).isEqualTo(ManagedResourceStatus.DELETED);
        assertThat(workManager.exists(work)).isTrue();
    }

    private static Stream<Arguments> provideArgsForCreateTest() {
        Object[][] arguments = {
                { ManagedResourceStatus.ACCEPTED, ConnectorState.READY, true, RhoasTopicAccessType.CONSUMER, ManagedResourceStatus.READY },
                { ManagedResourceStatus.ACCEPTED, ConnectorState.FAILED, true, RhoasTopicAccessType.CONSUMER, ManagedResourceStatus.FAILED },
                { ManagedResourceStatus.PREPARING, ConnectorState.READY, true, RhoasTopicAccessType.CONSUMER, ManagedResourceStatus.READY },
                { ManagedResourceStatus.PREPARING, ConnectorState.FAILED, true, RhoasTopicAccessType.CONSUMER, ManagedResourceStatus.FAILED },
                { ManagedResourceStatus.ACCEPTED, ConnectorState.READY, false, RhoasTopicAccessType.PRODUCER, ManagedResourceStatus.READY },
                { ManagedResourceStatus.ACCEPTED, ConnectorState.FAILED, false, RhoasTopicAccessType.PRODUCER, ManagedResourceStatus.FAILED },
                { ManagedResourceStatus.PREPARING, ConnectorState.READY, false, RhoasTopicAccessType.PRODUCER, ManagedResourceStatus.READY },
                { ManagedResourceStatus.PREPARING, ConnectorState.FAILED, false, RhoasTopicAccessType.PRODUCER, ManagedResourceStatus.FAILED }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    private static Stream<Arguments> provideArgsForDeleteTest() {
        Object[][] arguments = {
                { ManagedResourceStatus.DEPROVISION, ConnectorState.READY, true, RhoasTopicAccessType.CONSUMER },
                { ManagedResourceStatus.DEPROVISION, ConnectorState.FAILED, true, RhoasTopicAccessType.CONSUMER },
                { ManagedResourceStatus.DEPROVISION, ConnectorState.DELETED, true, RhoasTopicAccessType.CONSUMER },
                { ManagedResourceStatus.DELETING, ConnectorState.READY, true, RhoasTopicAccessType.CONSUMER },
                { ManagedResourceStatus.DELETING, ConnectorState.FAILED, true, RhoasTopicAccessType.CONSUMER },
                { ManagedResourceStatus.DELETING, ConnectorState.DELETED, true, RhoasTopicAccessType.CONSUMER },
                { ManagedResourceStatus.DEPROVISION, ConnectorState.READY, false, RhoasTopicAccessType.PRODUCER },
                { ManagedResourceStatus.DEPROVISION, ConnectorState.FAILED, false, RhoasTopicAccessType.PRODUCER },
                { ManagedResourceStatus.DEPROVISION, ConnectorState.DELETED, false, RhoasTopicAccessType.PRODUCER },
                { ManagedResourceStatus.DELETING, ConnectorState.READY, false, RhoasTopicAccessType.PRODUCER },
                { ManagedResourceStatus.DELETING, ConnectorState.FAILED, false, RhoasTopicAccessType.PRODUCER },
                { ManagedResourceStatus.DELETING, ConnectorState.DELETED, false, RhoasTopicAccessType.PRODUCER }
        };
        return Stream.of(arguments).map(Arguments::of);
    }
}
