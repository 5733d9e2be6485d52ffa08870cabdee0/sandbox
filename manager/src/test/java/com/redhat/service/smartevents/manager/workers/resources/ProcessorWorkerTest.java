package com.redhat.service.smartevents.manager.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.dao.ConnectorsDAO;
import com.redhat.service.smartevents.manager.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.manager.workers.resources.WorkerTestUtils.makeJobExecutionContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class ProcessorWorkerTest {

    private static final String TEST_RESOURCE_ID = "123";

    @InjectMock
    ConnectorWorker connectorWorker;

    @InjectMock(convertScopes = true)
    Scheduler quartzMock;

    @Inject
    ProcessorWorker worker;

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
        JobExecutionContext context = makeJobExecutionContext(TEST_RESOURCE_ID, 0, ZonedDateTime.now(ZoneOffset.UTC));

        assertThatCode(() -> worker.handleWork(context)).isInstanceOf(IllegalStateException.class);
    }

    @Transactional
    @ParameterizedTest
    @EnumSource(value = ManagedResourceStatus.class, names = { "ACCEPTED", "PREPARING" })
    void handleWorkProvisioningWithKnownResourceWithoutConnector(ManagedResourceStatus status) throws SchedulerException {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.READY);
        processor.setStatus(status);
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);

        JobExecutionContext context = makeJobExecutionContext(processor.getId(), 0, ZonedDateTime.now(ZoneOffset.UTC));

        Processor refreshed = worker.handleWork(context);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(ManagedResourceStatus.READY);
        verify(quartzMock, never()).rescheduleJob(any(), any());
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("srcHandleWorkProvisioningWithKnownResourceWithConnector")
    void handleWorkProvisioningWithKnownResourceWithConnector(ManagedResourceStatus status,
            ManagedResourceStatus statusWhenComplete,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean isWorkComplete) throws SchedulerException {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.READY);
        processor.setStatus(status);
        ConnectorEntity connectorEntity = Fixtures.createSinkConnector(processor, ManagedResourceStatus.ACCEPTED);
        connectorEntity.setStatus(ManagedResourceStatus.ACCEPTED);
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);
        connectorsDAO.persist(connectorEntity);

        JobExecutionContext context = makeJobExecutionContext(processor.getId(), 0, ZonedDateTime.now(ZoneOffset.UTC));

        doAnswer((i) -> {
            //Emulate ConnectorWorker completing work
            connectorEntity.setStatus(dependencyStatusWhenComplete);
            return connectorEntity;
        }).when(connectorWorker).createDependencies(context, connectorEntity);

        Processor refreshed = worker.handleWork(context);

        assertThat(refreshed.getStatus()).isEqualTo(statusWhenComplete);
        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);

        verify(connectorWorker).createDependencies(context, connectorEntity);

        verify(quartzMock, times(isWorkComplete ? 0 : 1)).rescheduleJob(any(), any());
    }

    private static Stream<Arguments> srcHandleWorkProvisioningWithKnownResourceWithConnector() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.PREPARING, ManagedResourceStatus.READY, true),
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.FAILED, ManagedResourceStatus.FAILED, true),
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.PREPARING, ManagedResourceStatus.PROVISIONING, false),
                Arguments.of(ManagedResourceStatus.PREPARING, ManagedResourceStatus.PREPARING, ManagedResourceStatus.READY, true),
                Arguments.of(ManagedResourceStatus.PREPARING, ManagedResourceStatus.FAILED, ManagedResourceStatus.FAILED, true),
                Arguments.of(ManagedResourceStatus.PREPARING, ManagedResourceStatus.PREPARING, ManagedResourceStatus.PROVISIONING, false));
    }

    @Transactional
    @ParameterizedTest
    @EnumSource(value = ManagedResourceStatus.class, names = { "DEPROVISION", "DELETING" })
    void handleWorkDeletingWithKnownResourceWithoutConnector(ManagedResourceStatus status) throws SchedulerException {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.READY);
        processor.setStatus(status);
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);

        JobExecutionContext context = makeJobExecutionContext(processor.getId(), 0, ZonedDateTime.now(ZoneOffset.UTC));

        Processor refreshed = worker.handleWork(context);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(ManagedResourceStatus.DELETED);
        verify(quartzMock, never()).rescheduleJob(any(), any());
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("srcHandleWorkDeletingWithKnownResourceWithConnector")
    void handleWorkDeletingWithKnownResourceWithConnector(ManagedResourceStatus status,
            ManagedResourceStatus statusWhenComplete,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean isWorkComplete) throws SchedulerException {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.READY);
        processor.setStatus(status);
        ConnectorEntity connectorEntity = Fixtures.createSinkConnector(processor, ManagedResourceStatus.ACCEPTED);
        connectorEntity.setStatus(ManagedResourceStatus.ACCEPTED);
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);
        connectorsDAO.persist(connectorEntity);

        JobExecutionContext context = makeJobExecutionContext(processor.getId(), 0, ZonedDateTime.now(ZoneOffset.UTC));

        doAnswer((i) -> {
            //Emulate ConnectorWorker completing work
            connectorEntity.setStatus(dependencyStatusWhenComplete);
            return connectorEntity;
        }).when(connectorWorker).deleteDependencies(context, connectorEntity);

        Processor refreshed = worker.handleWork(context);

        assertThat(refreshed.getStatus()).isEqualTo(statusWhenComplete);
        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);

        verify(connectorWorker).deleteDependencies(context, connectorEntity);

        verify(quartzMock, times(isWorkComplete ? 0 : 1)).rescheduleJob(any(), any());
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
