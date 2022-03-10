package com.redhat.service.bridge.manager.workers.resources;

import java.time.ZonedDateTime;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
public class ConnectorWorkerTest {

    private static final String RESOURCE_ID = "123";

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
        work.setManagedResourceId(RESOURCE_ID);

        when(connectorsDAO.findById(RESOURCE_ID)).thenReturn(null);

        assertThatCode(() -> worker.handleWork(work)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @EnumSource(value = ManagedResourceStatus.class, names = { "ACCEPTED", "PROVISIONING" })
    void handleWorkProvisioningWithKnownResourceMultiplePasses(ManagedResourceStatus status) {
        Work work = new Work();
        work.setManagedResourceId(RESOURCE_ID);
        work.setSubmittedAt(ZonedDateTime.now());

        ConnectorEntity connectorEntity = spy(new ConnectorEntity());
        connectorEntity.setStatus(status);
        Connector connector = new Connector();
        connector.setStatus(new ConnectorStatusStatus().state(ConnectorState.READY));

        when(connectorsDAO.findById(RESOURCE_ID)).thenReturn(connectorEntity);
        when(connectorsDAO.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(connectorEntity)).thenReturn(connectorEntity);
        // Managed Connector will not be available immediately
        when(connectorsApi.getConnector(connectorEntity)).thenReturn(null, connector);

        worker.handleWork(work);

        verify(rhoasService).createTopicAndGrantAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);
        verify(connectorsApi).createConnector(connectorEntity);
        assertThat(connectorEntity.getStatus()).isEqualTo(ManagedResourceStatus.PROVISIONING);
        assertThat(connectorEntity.getDependencyStatus()).isEqualTo(ManagedResourceStatus.PROVISIONING);

        // This emulates a subsequent invocation by WorkManager
        worker.handleWork(work);

        verify(rhoasService, times(2)).createTopicAndGrantAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);
        verify(connectorsApi, atMostOnce()).createConnector(connectorEntity);
        assertThat(connectorEntity.getPublishedAt()).isNotNull();
        assertThat(connectorEntity.getStatus()).isEqualTo(ManagedResourceStatus.READY);
        assertThat(connectorEntity.getDependencyStatus()).isEqualTo(ManagedResourceStatus.READY);

        verify(workManager, never()).complete(work);

        verify(connectorEntity, atLeastOnce()).setModifiedAt(any(ZonedDateTime.class));
    }

    @ParameterizedTest
    @EnumSource(value = ManagedResourceStatus.class, names = { "DEPROVISION", "DELETING" })
    void handleWorkDeletingWithKnownResourceMultiplePasses(ManagedResourceStatus status) {
        Work work = new Work();
        work.setManagedResourceId(RESOURCE_ID);
        work.setSubmittedAt(ZonedDateTime.now());

        ConnectorEntity connectorEntity = spy(new ConnectorEntity());
        connectorEntity.setStatus(status);
        connectorEntity.setTopicName("topicName");
        connectorEntity.setConnectorExternalId("connectorExternalId");
        Connector connector = new Connector();
        connector.setStatus(new ConnectorStatusStatus().state(ConnectorState.READY));

        when(connectorsDAO.findById(RESOURCE_ID)).thenReturn(connectorEntity);
        when(connectorsDAO.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(connectorEntity)).thenReturn(connectorEntity);
        // Managed Connector will initially be available before it is deleted
        when(connectorsApi.getConnector(connectorEntity)).thenReturn(connector, null);

        worker.handleWork(work);

        assertThat(connectorEntity.getStatus()).isEqualTo(ManagedResourceStatus.DELETING);
        assertThat(connectorEntity.getDependencyStatus()).isEqualTo(ManagedResourceStatus.DELETING);
        verify(rhoasService, never()).deleteTopicAndRevokeAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);
        verify(connectorsApi).deleteConnector(connectorEntity.getConnectorExternalId());

        // This emulates a subsequent invocation by WorkManager
        worker.handleWork(work);

        assertThat(connectorEntity.getStatus()).isEqualTo(ManagedResourceStatus.DELETED);
        assertThat(connectorEntity.getDependencyStatus()).isEqualTo(ManagedResourceStatus.DELETED);
        verify(rhoasService).deleteTopicAndRevokeAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);
        verify(connectorsDAO).deleteById(connectorEntity.getId());
        verify(workManager, never()).complete(work);

        verify(connectorEntity, atLeastOnce()).setModifiedAt(any(ZonedDateTime.class));
    }

}
