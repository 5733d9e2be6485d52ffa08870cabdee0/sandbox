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

import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.models.Work;
import com.redhat.service.bridge.manager.workers.WorkManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProcessorWorkerTest {

    private static final String RESOURCE_ID = "123";

    @Mock
    ProcessorDAO processorDAO;

    @Mock
    ConnectorsDAO connectorsDAO;

    @Mock
    WorkManager workManager;

    @Mock
    EntityManager entityManager;

    private ProcessorWorker worker;

    @BeforeEach
    void setup() {
        this.worker = new ProcessorWorker();
        this.worker.processorDAO = this.processorDAO;
        this.worker.connectorsDAO = this.connectorsDAO;
        this.worker.workManager = this.workManager;
        this.worker.maxRetries = 3;
        this.worker.timeoutSeconds = 60;
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

        Processor processor = spy(new Processor());
        processor.setStatus(status);

        when(processorDAO.findById(RESOURCE_ID)).thenReturn(processor);
        when(processorDAO.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(processor)).thenReturn(processor);
        when(connectorsDAO.findByProcessorId(anyString())).thenReturn(null);

        worker.handleWork(work);

        assertThat(processor.getStatus()).isEqualTo(status);
        assertThat(processor.getDependencyStatus()).isEqualTo(ManagedResourceStatus.READY);
        verify(processor, atLeastOnce()).setModifiedAt(any(ZonedDateTime.class));
        verify(workManager, never()).complete(work);
    }

    @ParameterizedTest
    @MethodSource("srcHandleWorkProvisioningWithKnownResourceWithConnector")
    void handleWorkProvisioningWithKnownResourceWithConnector(ManagedResourceStatus status,
            ManagedResourceStatus statusWhenComplete,
            ManagedResourceStatus dependencyStatusWhenComplete) {
        Work work = new Work();
        work.setManagedResourceId(RESOURCE_ID);
        work.setSubmittedAt(ZonedDateTime.now());

        Processor processor = new Processor();
        processor.setStatus(status);
        ConnectorEntity connector = new ConnectorEntity();
        connector.setStatus(dependencyStatusWhenComplete);

        when(processorDAO.findById(RESOURCE_ID)).thenReturn(processor);
        when(processorDAO.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(processor)).thenReturn(processor);
        when(connectorsDAO.findByProcessorId(anyString())).thenReturn(connector);

        worker.handleWork(work);

        assertThat(processor.getStatus()).isEqualTo(statusWhenComplete);
        assertThat(processor.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        verify(workManager, never()).complete(any(Work.class));
    }

    private static Stream<Arguments> srcHandleWorkProvisioningWithKnownResourceWithConnector() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.READY),
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.FAILED, ManagedResourceStatus.FAILED),
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.PROVISIONING),
                Arguments.of(ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.READY),
                Arguments.of(ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.FAILED, ManagedResourceStatus.FAILED),
                Arguments.of(ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.PROVISIONING));
    }

    @ParameterizedTest
    @EnumSource(value = ManagedResourceStatus.class, names = { "DEPROVISION", "DELETING" })
    void handleWorkDeletingWithKnownResourceWithoutConnector(ManagedResourceStatus status) {
        Work work = new Work();
        work.setManagedResourceId(RESOURCE_ID);
        work.setSubmittedAt(ZonedDateTime.now());

        Processor processor = spy(new Processor());
        processor.setStatus(status);

        when(processorDAO.findById(RESOURCE_ID)).thenReturn(processor);
        when(processorDAO.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(processor)).thenReturn(processor);

        worker.handleWork(work);

        assertThat(processor.getStatus()).isEqualTo(status);
        assertThat(processor.getDependencyStatus()).isEqualTo(ManagedResourceStatus.DELETED);
        verify(processor, atLeastOnce()).setModifiedAt(any(ZonedDateTime.class));
        verify(workManager, never()).complete(work);
    }

    @ParameterizedTest
    @MethodSource("srcHandleWorkDeletingWithKnownResourceWithConnector")
    void handleWorkDeletingWithKnownResourceWithConnector(ManagedResourceStatus status,
            ManagedResourceStatus statusWhenComplete,
            ManagedResourceStatus dependencyStatusWhenComplete) {
        Work work = new Work();
        work.setManagedResourceId(RESOURCE_ID);
        work.setSubmittedAt(ZonedDateTime.now());

        Processor processor = spy(new Processor());
        processor.setStatus(status);
        ConnectorEntity connector = new ConnectorEntity();
        connector.setStatus(dependencyStatusWhenComplete);

        when(processorDAO.findById(RESOURCE_ID)).thenReturn(processor);
        when(processorDAO.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(processor)).thenReturn(processor);
        when(connectorsDAO.findByProcessorId(processor.getId())).thenReturn(connector);

        worker.handleWork(work);

        assertThat(processor.getStatus()).isEqualTo(statusWhenComplete);
        assertThat(processor.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        verify(processor, atLeastOnce()).setModifiedAt(any(ZonedDateTime.class));
    }

    private static Stream<Arguments> srcHandleWorkDeletingWithKnownResourceWithConnector() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETED),
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.FAILED, ManagedResourceStatus.FAILED),
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETING),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETED),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.FAILED, ManagedResourceStatus.FAILED),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETING));
    }

}
