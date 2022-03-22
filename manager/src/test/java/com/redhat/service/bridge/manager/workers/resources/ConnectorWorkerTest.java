package com.redhat.service.bridge.manager.workers.resources;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorState;
import com.openshift.cloud.api.connector.models.ConnectorStatusStatus;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.connectors.ConnectorsApiClient;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Work;
import com.redhat.service.bridge.manager.workers.WorkManager;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectorWorkerTest {

    private static final String TEST_CONNECTOR_EXTERNAL_ID = "connectorExternalId";
    private static final String TEST_RESOURCE_ID = "123";
    private static final String TEST_TOPIC_NAME = "topicName";

    @Mock
    ConnectorsDAO connectorsDAO;

    @Mock
    RhoasService rhoasService;

    @Mock
    ConnectorsApiClient connectorsApi;

    @Mock
    WorkManager workManager;

    @Mock
    EntityManager entityManager;

    private ConnectorWorker worker;

    @BeforeEach
    void setup() {
        this.worker = new ConnectorWorker();
        this.worker.connectorsDAO = this.connectorsDAO;
        this.worker.rhoasService = this.rhoasService;
        this.worker.connectorsApi = this.connectorsApi;
        this.worker.workManager = this.workManager;
        this.worker.maxRetries = 3;
        this.worker.timeoutSeconds = 60;
    }

    @Test
    void handleWorkProvisioningWithUnknownResource() {
        Work work = new Work();
        work.setManagedResourceId(TEST_RESOURCE_ID);

        when(connectorsDAO.findById(TEST_RESOURCE_ID)).thenReturn(null);

        assertThatCode(() -> worker.handleWork(work)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @MethodSource("provideArgsForCreateTest")
    void handleWorkProvisioningWithKnownResourceMultiplePasses(
            ManagedResourceStatus resourceStatus,
            ConnectorState connectorState,
            ManagedResourceStatus expectedResourceStatus) {
        Work work = new Work();
        work.setManagedResourceId(TEST_RESOURCE_ID);
        work.setSubmittedAt(ZonedDateTime.now());

        ConnectorEntity connectorEntity = spy(new ConnectorEntity());
        connectorEntity.setStatus(resourceStatus);
        Connector connector = new Connector();
        connector.setId(TEST_CONNECTOR_EXTERNAL_ID);
        connector.setStatus(new ConnectorStatusStatus().state(connectorState));

        when(connectorsDAO.findById(TEST_RESOURCE_ID)).thenReturn(connectorEntity);
        when(connectorsDAO.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(connectorEntity)).thenReturn(connectorEntity);
        when(connectorsApi.getConnector(TEST_CONNECTOR_EXTERNAL_ID)).thenReturn(connector);
        when(connectorsApi.createConnector(connectorEntity)).thenReturn(connector);

        worker.handleWork(work);

        verify(rhoasService).createTopicAndGrantAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);
        verify(connectorsApi).createConnector(connectorEntity);
        assertThat(connectorEntity.getStatus()).isEqualTo(ManagedResourceStatus.PROVISIONING);
        assertThat(connectorEntity.getDependencyStatus()).isEqualTo(ManagedResourceStatus.PROVISIONING);

        // This emulates a subsequent invocation by WorkManager
        worker.handleWork(work);

        verify(rhoasService, times(2)).createTopicAndGrantAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);
        verify(connectorsApi, atMostOnce()).createConnector(connectorEntity);
        assertThat(connectorEntity.getStatus()).isEqualTo(expectedResourceStatus);
        assertThat(connectorEntity.getDependencyStatus()).isEqualTo(expectedResourceStatus);
        if (expectedResourceStatus == ManagedResourceStatus.READY) {
            assertThat(connectorEntity.getPublishedAt()).isNotNull();
        } else {
            assertThat(connectorEntity.getPublishedAt()).isNull();
        }

        verify(workManager, never()).complete(work);

        verify(connectorEntity, atLeastOnce()).setModifiedAt(any(ZonedDateTime.class));
    }

    @ParameterizedTest
    @MethodSource("provideArgsForDeleteTest")
    void handleWorkDeletingWithKnownResourceMultiplePasses(
            ManagedResourceStatus resourceStatus,
            ConnectorState connectorState) {
        Work work = new Work();
        work.setManagedResourceId(TEST_RESOURCE_ID);
        work.setSubmittedAt(ZonedDateTime.now());

        ConnectorEntity connectorEntity = spy(new ConnectorEntity());
        connectorEntity.setStatus(resourceStatus);
        connectorEntity.setTopicName(TEST_TOPIC_NAME);
        connectorEntity.setConnectorExternalId(TEST_CONNECTOR_EXTERNAL_ID);
        Connector connector = new Connector();
        connector.setStatus(new ConnectorStatusStatus().state(connectorState));

        when(connectorsDAO.findById(TEST_RESOURCE_ID)).thenReturn(connectorEntity);
        when(connectorsDAO.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(connectorEntity)).thenReturn(connectorEntity);
        // Managed Connector will initially be available before it is deleted
        when(connectorsApi.getConnector(connectorEntity.getConnectorExternalId())).thenReturn(connector, null);

        worker.handleWork(work);

        if (connectorState != ConnectorState.DELETED) {
            assertThat(connectorEntity.getStatus()).isEqualTo(ManagedResourceStatus.DELETING);
            assertThat(connectorEntity.getDependencyStatus()).isEqualTo(ManagedResourceStatus.DELETING);
            verify(rhoasService, never()).deleteTopicAndRevokeAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);
            verify(connectorsApi).deleteConnector(connectorEntity.getConnectorExternalId());

            // This emulates a subsequent invocation by WorkManager
            worker.handleWork(work);
        }

        assertThat(connectorEntity.getStatus()).isEqualTo(ManagedResourceStatus.DELETED);
        assertThat(connectorEntity.getDependencyStatus()).isEqualTo(ManagedResourceStatus.DELETED);
        verify(rhoasService).deleteTopicAndRevokeAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);
        verify(connectorsDAO).deleteById(connectorEntity.getId());
        verify(workManager, never()).complete(work);

        verify(connectorEntity, atLeastOnce()).setModifiedAt(any(ZonedDateTime.class));
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
