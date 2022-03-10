package com.redhat.service.bridge.manager.workers.resources;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorState;
import com.openshift.cloud.api.connector.models.ConnectorStatusStatus;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.ConnectorGetException;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.connectors.ConnectorsApiClient;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.models.Work;
import com.redhat.service.bridge.manager.workers.WorkManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProcessorChainWorkerTest {

    private static final String RESOURCE_ID = "123";

    @Mock
    ProcessorDAO processorDAO;

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

    private ProcessorChainWorker worker;

    private ProcessorWorker processorWorker;

    private ConnectorWorker connectorWorker;

    @BeforeEach
    void setup() {
        this.worker = new ProcessorChainWorker();
        this.processorWorker = new ProcessorWorker();
        this.connectorWorker = new ConnectorWorker();
        this.processorWorker.processorDAO = this.processorDAO;
        this.processorWorker.connectorsDAO = this.connectorsDAO;
        this.processorWorker.workManager = this.workManager;
        this.processorWorker.maxRetries = 3;
        this.processorWorker.timeoutSeconds = 60;
        this.connectorWorker.connectorsDAO = this.connectorsDAO;
        this.connectorWorker.rhoasService = this.rhoasService;
        this.connectorWorker.connectorsApi = this.connectorsApi;
        this.connectorWorker.workManager = this.workManager;
        this.connectorWorker.maxRetries = 3;
        this.connectorWorker.timeoutSeconds = 60;
        this.worker.processorDAO = this.processorDAO;
        this.worker.connectorsDAO = this.connectorsDAO;
        this.worker.processorWorker = this.processorWorker;
        this.worker.connectorWorker = this.connectorWorker;
        this.worker.workManager = this.workManager;
    }

    @Test
    void handleWorkProvisioningWithUnknownResource() {
        Work work = new Work();
        work.setManagedResourceId(RESOURCE_ID);

        when(processorDAO.findById(RESOURCE_ID)).thenReturn(null);

        assertThatCode(() -> worker.handleWork(work)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @EnumSource(value = ManagedResourceStatus.class, names = { "ACCEPTED", "PROVISIONING" })
    void handleWorkProvisioningWithKnownResourceWithoutConnector(ManagedResourceStatus status) {
        Work work = new Work();
        work.setManagedResourceId(RESOURCE_ID);
        work.setSubmittedAt(ZonedDateTime.now());

        Processor processor = new Processor();
        processor.setId(RESOURCE_ID);
        processor.setStatus(status);

        when(processorDAO.findById(RESOURCE_ID)).thenReturn(processor);
        when(processorDAO.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(processor)).thenReturn(processor);

        assertThat(worker.handleWork(work)).isTrue();

        assertThat(processor.getDependencyStatus()).isEqualTo(ManagedResourceStatus.READY);
    }

    @ParameterizedTest
    @MethodSource("srcHandleWorkProvisioningWithKnownResourceWithConnector")
    void handleWorkProvisioningWithKnownResourceWithConnector(ManagedResourceStatus status,
            ManagedResourceStatus statusWhenComplete,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean isWorkComplete) {
        Work work = new Work();
        work.setManagedResourceId(RESOURCE_ID);
        work.setSubmittedAt(ZonedDateTime.now());

        Processor processor = new Processor();
        processor.setId(RESOURCE_ID);
        processor.setStatus(status);
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setStatus(ManagedResourceStatus.ACCEPTED);
        connectorEntity.setConnectorExternalId("connectorExternalId");
        Connector connector = new Connector();
        connector.setStatus(new ConnectorStatusStatus().state(ConnectorState.READY));

        when(processorDAO.findById(RESOURCE_ID)).thenReturn(processor);
        when(processorDAO.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(processor)).thenReturn(processor);
        when(connectorsDAO.findById(connectorEntity.getId())).thenReturn(connectorEntity);
        when(connectorsDAO.findByProcessorId(processor.getId())).thenReturn(connectorEntity);
        when(connectorsDAO.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(connectorEntity)).thenReturn(connectorEntity);

        switch (dependencyStatusWhenComplete) {
            case PROVISIONING:
                // Mock an exception when provisioning the Managed Connector
                when(connectorsApi.getConnector(connectorEntity.getConnectorExternalId())).thenThrow(new ConnectorGetException("error"));
                break;
            case FAILED:
                // Mock failure to get Managed Connector
                work.setAttempts(Integer.MAX_VALUE);
                break;
            default:
                when(connectorsApi.getConnector(connectorEntity.getConnectorExternalId())).thenReturn(connector);
        }

        worker.handleWork(work);

        assertThat(processor.getStatus()).isEqualTo(statusWhenComplete);
        assertThat(processor.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        verify(workManager, times(isWorkComplete ? 1 : 0)).complete(any(Work.class));
    }

    private static Stream<Arguments> srcHandleWorkProvisioningWithKnownResourceWithConnector() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.READY, true),
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.FAILED, ManagedResourceStatus.FAILED, true),
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.PROVISIONING, false),
                Arguments.of(ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.READY, true),
                Arguments.of(ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.FAILED, ManagedResourceStatus.FAILED, true),
                Arguments.of(ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.PROVISIONING, false));
    }

    @ParameterizedTest
    @EnumSource(value = ManagedResourceStatus.class, names = { "DEPROVISION", "DELETING" })
    void handleWorkDeletingWithKnownResourceWithoutConnector(ManagedResourceStatus status) {
        Work work = new Work();
        work.setManagedResourceId(RESOURCE_ID);
        work.setSubmittedAt(ZonedDateTime.now());

        Processor processor = new Processor();
        processor.setId(RESOURCE_ID);
        processor.setStatus(status);

        when(processorDAO.findById(RESOURCE_ID)).thenReturn(processor);
        when(processorDAO.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(processor)).thenReturn(processor);

        worker.handleWork(work);

        assertThat(processor.getDependencyStatus()).isEqualTo(ManagedResourceStatus.DELETED);
        verify(workManager).complete(work);
    }

    @ParameterizedTest
    @MethodSource("srcHandleWorkDeletingWithKnownResourceWithConnector")
    void handleWorkDeletingWithKnownResourceWithConnector(ManagedResourceStatus status,
            ManagedResourceStatus statusWhenComplete,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean isWorkComplete) {
        Work work = new Work();
        work.setManagedResourceId(RESOURCE_ID);
        work.setSubmittedAt(ZonedDateTime.now());

        Processor processor = new Processor();
        processor.setId(RESOURCE_ID);
        processor.setStatus(status);
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setStatus(ManagedResourceStatus.DEPROVISION);
        connectorEntity.setConnectorExternalId("connectorExternalId");
        Connector connector = new Connector();
        connector.setStatus(new ConnectorStatusStatus().state(ConnectorState.DELETED));

        when(processorDAO.findById(RESOURCE_ID)).thenReturn(processor);
        when(processorDAO.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(processor)).thenReturn(processor);
        when(connectorsDAO.findById(connectorEntity.getId())).thenReturn(connectorEntity);
        when(connectorsDAO.findByProcessorId(processor.getId())).thenReturn(connectorEntity);
        when(connectorsDAO.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(connectorEntity)).thenReturn(connectorEntity);

        switch (dependencyStatusWhenComplete) {
            case DELETING:
                // Mock an exception when de-provisioning the Managed Connector
                when(connectorsApi.getConnector(connectorEntity.getConnectorExternalId())).thenThrow(new ConnectorGetException("error"));
                break;
            case FAILED:
                // Mock failure to get Managed Connector
                work.setAttempts(Integer.MAX_VALUE);
                break;
            default:
                when(connectorsApi.getConnector(connectorEntity.getConnectorExternalId())).thenReturn(connector);
        }

        worker.handleWork(work);

        assertThat(processor.getStatus()).isEqualTo(statusWhenComplete);
        assertThat(processor.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        verify(workManager, times(isWorkComplete ? 1 : 0)).complete(any(Work.class));
    }

    private static Stream<Arguments> srcHandleWorkDeletingWithKnownResourceWithConnector() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETED, true),
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.FAILED, ManagedResourceStatus.FAILED, true),
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETING, false),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETED, true),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.FAILED, ManagedResourceStatus.FAILED, true),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETING, false));
    }

}
